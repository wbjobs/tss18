package com.ticket.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.engine.dto.*;
import com.ticket.engine.engine.DistributedLock;
import com.ticket.engine.engine.RetryStrategy;
import com.ticket.engine.engine.SagaOrchestrator;
import com.ticket.engine.engine.SagaOrchestrator.SagaContext;
import com.ticket.engine.engine.StateMachineEngine;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.SagaCompensationLog;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.repository.TicketInstanceRepository;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final String EVENT_TICKET_CREATED = "TICKET_CREATED";
    private static final String EVENT_TICKET_UPDATED = "TICKET_UPDATED";
    private static final String EVENT_STATE_TRANSITION = "STATE_TRANSITION";

    private static final String STEP_VALIDATE = "VALIDATE";
    private static final String STEP_EVALUATE_CONDITION = "EVALUATE_CONDITION";
    private static final String STEP_UPDATE_STATE = "UPDATE_STATE";
    private static final String STEP_RECORD_TRACE = "RECORD_TRACE";
    private static final String STEP_NOTIFY_WEBHOOK = "NOTIFY_WEBHOOK";

    private static final int OPTIMISTIC_LOCK_MAX_RETRIES = 3;

    @Autowired
    private TicketInstanceRepository ticketInstanceRepository;

    @Autowired
    private StateMachineService stateMachineService;

    @Autowired
    private StateMachineEngine stateMachineEngine;

    @Autowired
    private TraceService traceService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private SagaOrchestrator sagaOrchestrator;

    @Autowired
    private RetryStrategy retryStrategy;

    @Transactional
    public TicketInstance createTicket(CreateTicketRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        Long stateMachineId;
        try {
            stateMachineId = Long.parseLong(request.getStateMachineId());
        } catch (Exception e) {
            throw new RuntimeException("状态机ID格式错误", e);
        }

        StateMachineDefinition definition = stateMachineService.loadStateMachineDefinition(tenantId, stateMachineId);
        if (definition == null) {
            throw new RuntimeException("状态机定义不存在");
        }

        StateNode startState = stateMachineEngine.getStartState(definition);
        if (startState == null) {
            throw new RuntimeException("状态机没有定义起始状态");
        }

        String payloadJson = null;
        if (request.getPayload() != null && !request.getPayload().isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(request.getPayload());
            } catch (Exception e) {
                throw new RuntimeException("序列化payload失败", e);
            }
        }

        TicketInstance ticket = TicketInstance.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .tenantId(tenantId)
                .stateMachineId(stateMachineId)
                .title(request.getTitle())
                .businessKey(request.getBusinessKey())
                .currentStateId(startState.getId())
                .currentStateName(startState.getName())
                .payload(payloadJson)
                .version(0L)
                .build();

        TicketInstance saved = ticketInstanceRepository.save(ticket);

        webhookService.notifyWebhooks(tenantId, EVENT_TICKET_CREATED, saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public TicketInstance getTicket(Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        return ticketInstanceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));
    }

    @Transactional(readOnly = true)
    public PageResult<TicketInstance> listTickets(int page, int size, String state) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TicketInstance> pageResult;

        if (state != null && !state.trim().isEmpty()) {
            pageResult = ticketInstanceRepository.findByTenantIdAndCurrentStateId(tenantId, state, pageable);
        } else {
            pageResult = ticketInstanceRepository.findByTenantId(tenantId, pageable);
        }

        return PageResult.<TicketInstance>builder()
                .list(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .build();
    }

    @Transactional
    public TransitionResponse executeTransition(Long ticketId, TransitionRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        try (DistributedLock.LockHolder lockHolder = distributedLock.tryLock(ticketId)) {
            if (!lockHolder.isAcquired()) {
                log.warn("获取分布式锁失败，工单正在被其他操作处理: ticketId={}", ticketId);
                return TransitionResponse.builder()
                        .success(false)
                        .message("工单正在被其他操作处理，请稍后重试")
                        .build();
            }

            Object monitor = lockHolder.getLocalMonitor();
            if (monitor != null) {
                synchronized (monitor) {
                    return doExecuteTransition(tenantId, ticketId, request,
                        StateMachineEngine.TRIGGER_SOURCE_MANUAL, request.getRemark(),
                        0L, "system");
                }
            }
            return doExecuteTransition(tenantId, ticketId, request,
                StateMachineEngine.TRIGGER_SOURCE_MANUAL, request.getRemark(),
                0L, "system");
        }
    }

    @Transactional
    public TransitionResponse executeCallbackTransition(Long tenantId, String callbackSource,
                                                         CallbackRequest request) {
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        TicketInstance ticket = ticketInstanceRepository.findByTenantIdAndBusinessKey(tenantId, request.getBusinessKey())
                .orElseThrow(() -> new RuntimeException("工单不存在: businessKey=" + request.getBusinessKey()));

        try (DistributedLock.LockHolder lockHolder = distributedLock.tryLock(ticket.getId())) {
            if (!lockHolder.isAcquired()) {
                log.warn("获取分布式锁失败，工单正在被其他操作处理: ticketId={}, callbackSource={}",
                    ticket.getId(), callbackSource);
                return TransitionResponse.builder()
                        .success(false)
                        .message("工单正在被其他操作处理，请稍后重试")
                        .build();
            }

            Object monitor = lockHolder.getLocalMonitor();
            if (monitor != null) {
                synchronized (monitor) {
                    return doExecuteCallbackTransition(tenantId, ticket.getId(),
                        callbackSource, request);
                }
            }
            return doExecuteCallbackTransition(tenantId, ticket.getId(),
                callbackSource, request);
        }
    }

    private TransitionResponse doExecuteTransition(Long tenantId, Long ticketId,
                                                    TransitionRequest request,
                                                    String triggerSource,
                                                    String remark,
                                                    Long operatorId,
                                                    String operatorName) {
        int retryCount = 0;

        while (retryCount <= OPTIMISTIC_LOCK_MAX_RETRIES) {
            try {
                TicketInstance ticket = ticketInstanceRepository.findByIdAndTenantId(ticketId, tenantId)
                        .orElseThrow(() -> new RuntimeException("工单不存在"));

                SagaContext sagaContext = sagaOrchestrator.beginSaga(ticket);

                SagaCompensationLog validateLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_VALIDATE,
                    request.getTargetStateId(), "", null, triggerSource);

                StateMachineEngine.TransitionResult result;
                if (StateMachineEngine.TRIGGER_SOURCE_CALLBACK.equals(triggerSource)) {
                    result = executeTransitionWithRetry(ticket, triggerSource, request.getTriggerData());
                } else {
                    result = stateMachineEngine.executeTransition(
                        ticket, request.getTargetStateId(),
                        request.getTriggerData(), triggerSource);
                }

                if (!result.isSuccess()) {
                    sagaOrchestrator.failStep(validateLog, result.getMessage());
                    return TransitionResponse.builder()
                            .success(false)
                            .message(result.getMessage())
                            .build();
                }

                sagaOrchestrator.completeStep(validateLog);

                SagaCompensationLog conditionLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_EVALUATE_CONDITION,
                    result.getToState(), ticket.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    triggerSource);
                sagaOrchestrator.completeStep(conditionLog);

                String fromStateId = ticket.getCurrentStateId();
                String fromStateName = ticket.getCurrentStateName();

                SagaCompensationLog updateLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_UPDATE_STATE,
                    result.getToState(), ticket.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    triggerSource);

                TicketInstance saved = ticketInstanceRepository.save(ticket);
                sagaOrchestrator.completeStep(updateLog);

                SagaCompensationLog traceLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_RECORD_TRACE,
                    result.getToState(), saved.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    triggerSource);

                traceService.createTrace(
                    saved, fromStateId, fromStateName,
                    result.getToState(), saved.getCurrentStateName(),
                    triggerSource, remark, operatorId, operatorName);
                sagaOrchestrator.completeStep(traceLog);

                SagaCompensationLog webhookLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_NOTIFY_WEBHOOK,
                    result.getToState(), saved.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    triggerSource);

                notifyTransitionWebhooks(tenantId, saved, fromStateId, result, triggerSource, null);
                sagaOrchestrator.completeStep(webhookLog);

                log.info("状态转移完成: ticketId={}, {} -> {}, sagaId={}",
                    ticketId, fromStateId, result.getToState(), sagaContext.getSagaId());

                return TransitionResponse.builder()
                        .success(true)
                        .currentStateId(saved.getCurrentStateId())
                        .message(result.getMessage())
                        .build();

            } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                retryCount++;
                log.warn("乐观锁冲突，准备重试: ticketId={}, attempt={}/{}",
                    ticketId, retryCount, OPTIMISTIC_LOCK_MAX_RETRIES);

                if (retryCount > OPTIMISTIC_LOCK_MAX_RETRIES) {
                    log.error("乐观锁冲突重试耗尽: ticketId={}", ticketId);
                    return TransitionResponse.builder()
                            .success(false)
                            .message("并发冲突，请稍后重试")
                            .build();
                }

                try {
                    Thread.sleep(100L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return TransitionResponse.builder()
                            .success(false)
                            .message("操作被中断")
                            .build();
                }
            } catch (Exception e) {
                log.error("状态转移异常: ticketId={}, error={}", ticketId, e.getMessage(), e);
                try {
                    TicketInstance ticket = ticketInstanceRepository.findById(ticketId).orElse(null);
                    if (ticket != null) {
                        SagaContext sagaContext = sagaOrchestrator.beginSaga(ticket);
                        sagaOrchestrator.compensate(sagaContext);
                    }
                } catch (Exception compEx) {
                    log.error("Saga补偿失败: ticketId={}, error={}", ticketId, compEx.getMessage(), compEx);
                }

                return TransitionResponse.builder()
                        .success(false)
                        .message("状态转移失败: " + e.getMessage())
                        .build();
            }
        }

        return TransitionResponse.builder()
                .success(false)
                .message("并发冲突，请稍后重试")
                .build();
    }

    private TransitionResponse doExecuteCallbackTransition(Long tenantId, Long ticketId,
                                                            String callbackSource,
                                                            CallbackRequest request) {
        int retryCount = 0;

        while (retryCount <= OPTIMISTIC_LOCK_MAX_RETRIES) {
            try {
                TicketInstance ticket = ticketInstanceRepository.findByIdAndTenantId(ticketId, tenantId)
                        .orElseThrow(() -> new RuntimeException("工单不存在"));

                SagaContext sagaContext = sagaOrchestrator.beginSaga(ticket);

                SagaCompensationLog validateLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_VALIDATE,
                    "", "", null, StateMachineEngine.TRIGGER_SOURCE_CALLBACK);
                sagaOrchestrator.completeStep(validateLog);

                StateMachineEngine.TransitionResult result = retryStrategy.executeWithRetry(
                    () -> stateMachineEngine.executeCallbackTransition(
                        ticket, callbackSource, request.getData()),
                    "callback-transition-" + ticketId,
                    3,
                    RetryStrategy.isRetryableOnTimeout()
                );

                if (!result.isSuccess()) {
                    return TransitionResponse.builder()
                            .success(false)
                            .message(result.getMessage())
                            .build();
                }

                String fromStateId = ticket.getCurrentStateId();
                String fromStateName = ticket.getCurrentStateName();

                SagaCompensationLog conditionLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_EVALUATE_CONDITION,
                    result.getToState(), ticket.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK);
                sagaOrchestrator.completeStep(conditionLog);

                SagaCompensationLog updateLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_UPDATE_STATE,
                    result.getToState(), ticket.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK);

                TicketInstance saved = ticketInstanceRepository.save(ticket);
                sagaOrchestrator.completeStep(updateLog);

                SagaCompensationLog traceLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_RECORD_TRACE,
                    result.getToState(), saved.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK);

                traceService.createTrace(
                    saved, fromStateId, fromStateName,
                    result.getToState(), saved.getCurrentStateName(),
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK,
                    "回调触发: " + callbackSource,
                    0L, "callback:" + callbackSource);
                sagaOrchestrator.completeStep(traceLog);

                SagaCompensationLog webhookLog = sagaOrchestrator.recordStep(
                    sagaContext, STEP_NOTIFY_WEBHOOK,
                    result.getToState(), saved.getCurrentStateName(),
                    result.getTransition() != null ? result.getTransition().getId() : null,
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK);

                Map<String, Object> callbackData = new HashMap<>();
                callbackData.put("callbackSource", callbackSource);
                callbackData.put("callbackData", request.getData());
                notifyTransitionWebhooks(tenantId, saved, fromStateId, result,
                    StateMachineEngine.TRIGGER_SOURCE_CALLBACK, callbackData);
                sagaOrchestrator.completeStep(webhookLog);

                log.info("回调状态转移完成: ticketId={}, {} -> {}, callbackSource={}, sagaId={}",
                    ticketId, fromStateId, result.getToState(), callbackSource, sagaContext.getSagaId());

                return TransitionResponse.builder()
                        .success(true)
                        .currentStateId(saved.getCurrentStateId())
                        .message(result.getMessage())
                        .build();

            } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                retryCount++;
                log.warn("乐观锁冲突，准备重试: ticketId={}, attempt={}/{}, callbackSource={}",
                    ticketId, retryCount, OPTIMISTIC_LOCK_MAX_RETRIES, callbackSource);

                if (retryCount > OPTIMISTIC_LOCK_MAX_RETRIES) {
                    log.error("乐观锁冲突重试耗尽: ticketId={}, callbackSource={}", ticketId, callbackSource);
                    return TransitionResponse.builder()
                            .success(false)
                            .message("并发冲突，请稍后重试")
                            .build();
                }

                try {
                    Thread.sleep(100L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return TransitionResponse.builder()
                            .success(false)
                            .message("操作被中断")
                            .build();
                }
            } catch (RuntimeException e) {
                log.error("回调状态转移异常: ticketId={}, callbackSource={}, error={}",
                    ticketId, callbackSource, e.getMessage(), e);

                try {
                    TicketInstance ticket = ticketInstanceRepository.findById(ticketId).orElse(null);
                    if (ticket != null) {
                        SagaContext sagaContext = sagaOrchestrator.beginSaga(ticket);
                        sagaOrchestrator.compensate(sagaContext);
                    }
                } catch (Exception compEx) {
                    log.error("Saga补偿失败: ticketId={}, error={}", ticketId, compEx.getMessage(), compEx);
                }

                return TransitionResponse.builder()
                        .success(false)
                        .message("回调状态转移失败: " + e.getMessage())
                        .build();
            }
        }

        return TransitionResponse.builder()
                .success(false)
                .message("并发冲突，请稍后重试")
                .build();
    }

    private StateMachineEngine.TransitionResult executeTransitionWithRetry(
            TicketInstance ticket, String triggerSource, Map<String, Object> triggerData) {
        return retryStrategy.executeWithRetry(
            () -> stateMachineEngine.executeCallbackTransition(
                ticket, null, triggerData),
            "condition-evaluate-" + ticket.getId(),
            3,
            RetryStrategy.isRetryableOnTimeout()
        );
    }

    private void notifyTransitionWebhooks(Long tenantId, TicketInstance saved,
                                           String fromStateId,
                                           StateMachineEngine.TransitionResult result,
                                           String triggerSource,
                                           Map<String, Object> extraData) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("ticket", saved);
            eventPayload.put("fromState", fromStateId);
            eventPayload.put("toState", result.getToState());
            eventPayload.put("transition", result.getTransition());
            eventPayload.put("triggerSource", triggerSource);
            if (extraData != null) {
                eventPayload.putAll(extraData);
            }

            retryStrategy.executeWithRetry(
                () -> {
                    webhookService.notifyWebhooks(tenantId, EVENT_STATE_TRANSITION, eventPayload);
                    webhookService.notifyWebhooks(tenantId, EVENT_TICKET_UPDATED, saved);
                    return null;
                },
                "webhook-notify-" + saved.getId(),
                2,
                RetryStrategy.isRetryableOnTimeout()
            );
        } catch (Exception e) {
            log.warn("Webhook通知失败(不影响主流程): ticketId={}, error={}",
                saved.getId(), e.getMessage());
        }
    }
}
