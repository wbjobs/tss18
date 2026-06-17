package com.ticket.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.engine.dto.*;
import com.ticket.engine.engine.StateMachineEngine;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.repository.TicketInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private static final String EVENT_TICKET_CREATED = "TICKET_CREATED";
    private static final String EVENT_TICKET_UPDATED = "TICKET_UPDATED";
    private static final String EVENT_STATE_TRANSITION = "STATE_TRANSITION";

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

        TicketInstance ticket = ticketInstanceRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));

        String fromStateId = ticket.getCurrentStateId();
        String fromStateName = ticket.getCurrentStateName();

        StateMachineEngine.TransitionResult result = stateMachineEngine.executeTransition(
                ticket,
                request.getTargetStateId(),
                request.getTriggerData(),
                StateMachineEngine.TRIGGER_SOURCE_MANUAL
        );

        if (!result.isSuccess()) {
            return TransitionResponse.builder()
                    .success(false)
                    .message(result.getMessage())
                    .build();
        }

        TicketInstance saved = ticketInstanceRepository.save(ticket);

        Long operatorId = 0L;
        String operatorName = "system";

        traceService.createTrace(
                ticket,
                fromStateId,
                fromStateName,
                result.getToState(),
                ticket.getCurrentStateName(),
                StateMachineEngine.TRIGGER_SOURCE_MANUAL,
                request.getRemark(),
                operatorId,
                operatorName
        );

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("ticket", saved);
        eventPayload.put("fromState", fromStateId);
        eventPayload.put("toState", result.getToState());
        eventPayload.put("transition", result.getTransition());

        webhookService.notifyWebhooks(tenantId, EVENT_STATE_TRANSITION, eventPayload);
        webhookService.notifyWebhooks(tenantId, EVENT_TICKET_UPDATED, saved);

        return TransitionResponse.builder()
                .success(true)
                .currentStateId(saved.getCurrentStateId())
                .message(result.getMessage())
                .build();
    }

    @Transactional
    public TransitionResponse executeCallbackTransition(Long tenantId, String callbackSource, CallbackRequest request) {
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }

        TicketInstance ticket = ticketInstanceRepository.findByTenantIdAndBusinessKey(tenantId, request.getBusinessKey())
                .orElseThrow(() -> new RuntimeException("工单不存在"));

        String fromStateId = ticket.getCurrentStateId();
        String fromStateName = ticket.getCurrentStateName();

        StateMachineEngine.TransitionResult result = stateMachineEngine.executeCallbackTransition(
                ticket,
                callbackSource,
                request.getData()
        );

        if (!result.isSuccess()) {
            return TransitionResponse.builder()
                    .success(false)
                    .message(result.getMessage())
                    .build();
        }

        TicketInstance saved = ticketInstanceRepository.save(ticket);

        traceService.createTrace(
                ticket,
                fromStateId,
                fromStateName,
                result.getToState(),
                ticket.getCurrentStateName(),
                StateMachineEngine.TRIGGER_SOURCE_CALLBACK,
                "回调触发: " + callbackSource,
                0L,
                "callback:" + callbackSource
        );

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("ticket", saved);
        eventPayload.put("fromState", fromStateId);
        eventPayload.put("toState", result.getToState());
        eventPayload.put("transition", result.getTransition());
        eventPayload.put("callbackSource", callbackSource);
        eventPayload.put("callbackData", request.getData());

        webhookService.notifyWebhooks(tenantId, EVENT_STATE_TRANSITION, eventPayload);
        webhookService.notifyWebhooks(tenantId, EVENT_TICKET_UPDATED, saved);

        return TransitionResponse.builder()
                .success(true)
                .currentStateId(saved.getCurrentStateId())
                .message(result.getMessage())
                .build();
    }
}
