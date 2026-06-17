package com.ticket.engine.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.engine.dto.StateMachineDefinition;
import com.ticket.engine.dto.StateNode;
import com.ticket.engine.dto.Transition;
import com.ticket.engine.entity.SagaCompensationLog;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.repository.SagaCompensationLogRepository;
import com.ticket.engine.repository.TicketInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private static final String STEP_VALIDATE = "VALIDATE";
    private static final String STEP_EVALUATE_CONDITION = "EVALUATE_CONDITION";
    private static final String STEP_UPDATE_STATE = "UPDATE_STATE";
    private static final String STEP_RECORD_TRACE = "RECORD_TRACE";
    private static final String STEP_NOTIFY_WEBHOOK = "NOTIFY_WEBHOOK";

    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_COMPENSATING = "COMPENSATING";
    private static final String STATUS_COMPENSATED = "COMPENSATED";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    private SagaCompensationLogRepository sagaLogRepository;

    @Autowired
    private TicketInstanceRepository ticketInstanceRepository;

    @Autowired
    private StateMachineEngine stateMachineEngine;

    @Autowired
    private StateMachineCache stateMachineCache;

    @Autowired
    private ExpressionEvaluator expressionEvaluator;

    @Autowired
    private RetryStrategy retryStrategy;

    @Autowired
    private ObjectMapper objectMapper;

    public SagaContext beginSaga(TicketInstance ticket) {
        String sagaId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SagaContext context = new SagaContext(
            sagaId, ticket.getId(), ticket.getTenantId(),
            ticket.getCurrentStateId(), ticket.getCurrentStateName(),
            ticket.getVersion(), ticket.getPayload()
        );
        log.info("Saga开始: sagaId={}, ticketId={}, fromState={}",
            sagaId, ticket.getId(), ticket.getCurrentStateId());
        return context;
    }

    public SagaCompensationLog recordStep(SagaContext context, String step,
                                            String toStateId, String toStateName,
                                            String transitionId, String triggerSource) {
        SagaCompensationLog logEntry = SagaCompensationLog.builder()
            .ticketId(context.getTicketId())
            .tenantId(context.getTenantId())
            .sagaId(context.getSagaId())
            .step(step)
            .status(STATUS_STARTED)
            .fromStateId(context.getFromStateId())
            .fromStateName(context.getFromStateName())
            .toStateId(toStateId)
            .toStateName(toStateName)
            .transitionId(transitionId)
            .triggerSource(triggerSource)
            .snapshotPayload(context.getSnapshotPayload())
            .snapshotVersion(context.getSnapshotVersion())
            .build();

        return sagaLogRepository.save(logEntry);
    }

    public void completeStep(SagaCompensationLog logEntry) {
        logEntry.setStatus(STATUS_COMPLETED);
        sagaLogRepository.save(logEntry);
    }

    public void failStep(SagaCompensationLog logEntry, String errorMessage) {
        logEntry.setStatus(STATUS_FAILED);
        logEntry.setErrorMessage(errorMessage);
        sagaLogRepository.save(logEntry);
    }

    @Transactional
    public void compensate(SagaContext context) {
        log.warn("Saga补偿开始: sagaId={}, ticketId={}",
            context.getSagaId(), context.getTicketId());

        List<SagaCompensationLog> steps = sagaLogRepository
            .findBySagaIdOrderByCreatedAtAsc(context.getSagaId());

        for (int i = steps.size() - 1; i >= 0; i--) {
            SagaCompensationLog step = steps.get(i);
            if (!STATUS_COMPLETED.equals(step.getStatus()) && !STATUS_FAILED.equals(step.getStatus())) {
                continue;
            }

            try {
                compensateStep(context, step);
            } catch (Exception e) {
                log.error("Saga补偿步骤失败: sagaId={}, step={}, error={}",
                    context.getSagaId(), step.getStep(), e.getMessage(), e);
            }
        }

        rollbackTicketState(context);
    }

    private void compensateStep(SagaContext context, SagaCompensationLog step) {
        step.setStatus(STATUS_COMPENSATING);
        sagaLogRepository.save(step);

        switch (step.getStep()) {
            case STEP_NOTIFY_WEBHOOK:
                log.info("补偿: 忽略Webhook通知(已发送无法撤回), sagaId={}", context.getSagaId());
                break;

            case STEP_RECORD_TRACE:
                log.info("补偿: 保留轨迹记录(审计需要), sagaId={}", context.getSagaId());
                break;

            case STEP_UPDATE_STATE:
                rollbackTicketState(context);
                break;

            case STEP_EVALUATE_CONDITION:
                log.info("补偿: 条件评估无需回滚, sagaId={}", context.getSagaId());
                break;

            case STEP_VALIDATE:
                log.info("补偿: 校验步骤无需回滚, sagaId={}", context.getSagaId());
                break;

            default:
                log.warn("补偿: 未知步骤类型: {}", step.getStep());
        }

        step.setStatus(STATUS_COMPENSATED);
        step.setCompensatedAt(LocalDateTime.now());
        sagaLogRepository.save(step);
    }

    private void rollbackTicketState(SagaContext context) {
        try {
            TicketInstance ticket = ticketInstanceRepository.findById(context.getTicketId()).orElse(null);
            if (ticket == null) {
                log.error("回滚失败: 工单不存在, ticketId={}", context.getTicketId());
                return;
            }

            String currentState = ticket.getCurrentStateId();
            if (currentState.equals(context.getFromStateId())) {
                log.info("工单状态已与预期一致，无需回滚: ticketId={}, state={}",
                    context.getTicketId(), currentState);
                return;
            }

            ticket.setCurrentStateId(context.getFromStateId());
            ticket.setCurrentStateName(context.getFromStateName());

            if (context.getSnapshotPayload() != null) {
                ticket.setPayload(context.getSnapshotPayload());
            }

            ticket.setUpdatedAt(LocalDateTime.now());
            ticketInstanceRepository.save(ticket);

            stateMachineCache.evict(ticket.getTenantId(), ticket.getStateMachineId());

            log.info("状态回滚成功: ticketId={}, {} -> {}",
                context.getTicketId(), currentState, context.getFromStateId());
        } catch (Exception e) {
            log.error("状态回滚失败: ticketId={}, error={}",
                context.getTicketId(), e.getMessage(), e);
        }
    }

    public static class SagaContext {
        private final String sagaId;
        private final Long ticketId;
        private final Long tenantId;
        private final String fromStateId;
        private final String fromStateName;
        private final Long snapshotVersion;
        private final String snapshotPayload;

        public SagaContext(String sagaId, Long ticketId, Long tenantId,
                           String fromStateId, String fromStateName,
                           Long snapshotVersion, String snapshotPayload) {
            this.sagaId = sagaId;
            this.ticketId = ticketId;
            this.tenantId = tenantId;
            this.fromStateId = fromStateId;
            this.fromStateName = fromStateName;
            this.snapshotVersion = snapshotVersion;
            this.snapshotPayload = snapshotPayload;
        }

        public String getSagaId() { return sagaId; }
        public Long getTicketId() { return ticketId; }
        public Long getTenantId() { return tenantId; }
        public String getFromStateId() { return fromStateId; }
        public String getFromStateName() { return fromStateName; }
        public Long getSnapshotVersion() { return snapshotVersion; }
        public String getSnapshotPayload() { return snapshotPayload; }
    }
}
