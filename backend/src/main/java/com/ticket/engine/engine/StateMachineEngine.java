package com.ticket.engine.engine;

import com.ticket.engine.dto.StateMachineDefinition;
import com.ticket.engine.dto.StateNode;
import com.ticket.engine.dto.Transition;
import com.ticket.engine.entity.TicketInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StateMachineEngine {

    public static final String TRIGGER_SOURCE_CALLBACK = "CALLBACK";
    public static final String TRIGGER_SOURCE_MANUAL = "MANUAL";
    public static final String TRIGGER_SOURCE_AUTO = "AUTO";

    @Autowired
    private ExpressionEvaluator expressionEvaluator;

    @Autowired
    private StateMachineCache stateMachineCache;

    public TransitionResult executeTransition(TicketInstance ticket, String targetStateId,
                                              Map<String, Object> triggerData, String triggerSource) {
        if (ticket == null) {
            return TransitionResult.failure("工单实例不能为空");
        }

        StateMachineDefinition definition = loadStateMachineDefinition(ticket.getTenantId(), ticket.getStateMachineId());
        if (definition == null) {
            return TransitionResult.failure("状态机定义不存在");
        }

        String currentStateId = ticket.getCurrentStateId();
        Transition transition = findTransition(definition, currentStateId, targetStateId, triggerSource);
        if (transition == null) {
            return TransitionResult.failure(
                String.format("找不到从状态[%s]到[%s]的有效转移，触发源[%s]", currentStateId, targetStateId, triggerSource));
        }

        Map<String, Object> context = buildEvaluationContext(ticket, triggerData);

        try {
            if (!canTransitionInternal(definition, currentStateId, targetStateId, context)) {
                return TransitionResult.failure(
                    String.format("转移条件不满足，从[%s]到[%s]", currentStateId, targetStateId),
                    currentStateId, targetStateId, transition);
            }
        } catch (Exception e) {
            return TransitionResult.failure(
                String.format("转移条件校验失败: %s", e.getMessage()),
                currentStateId, targetStateId, transition);
        }

        StateNode targetNode = findStateNode(definition, targetStateId);
        if (targetNode != null) {
            ticket.setCurrentStateId(targetStateId);
            ticket.setCurrentStateName(targetNode.getName());
        }
        ticket.setUpdatedAt(LocalDateTime.now());

        return TransitionResult.success(currentStateId, targetStateId, transition);
    }

    public TransitionResult executeCallbackTransition(TicketInstance ticket, String callbackSource,
                                                      Map<String, Object> callbackData) {
        if (ticket == null) {
            return TransitionResult.failure("工单实例不能为空");
        }

        StateMachineDefinition definition = loadStateMachineDefinition(ticket.getTenantId(), ticket.getStateMachineId());
        if (definition == null) {
            return TransitionResult.failure("状态机定义不存在");
        }

        String currentStateId = ticket.getCurrentStateId();
        List<Transition> candidateTransitions = findCallbackTransitions(definition, currentStateId, callbackSource);

        if (candidateTransitions.isEmpty()) {
            return TransitionResult.failure(
                String.format("当前状态[%s]没有匹配的回调转移，回调源[%s]", currentStateId, callbackSource));
        }

        Map<String, Object> context = buildEvaluationContext(ticket, callbackData);

        for (Transition transition : candidateTransitions) {
            try {
                String condition = transition.getCondition();
                boolean conditionMet = (condition == null || condition.trim().isEmpty())
                    || expressionEvaluator.evaluate(condition, context);

                if (conditionMet) {
                    String targetStateId = transition.getTargetStateId();
                    StateNode targetNode = findStateNode(definition, targetStateId);
                    if (targetNode != null) {
                        ticket.setCurrentStateId(targetStateId);
                        ticket.setCurrentStateName(targetNode.getName());
                    }
                    ticket.setUpdatedAt(LocalDateTime.now());

                    return TransitionResult.success(currentStateId, targetStateId, transition);
                }
            } catch (Exception e) {
                continue;
            }
        }

        return TransitionResult.failure(
            String.format("当前状态[%s]没有满足条件的回调转移，回调源[%s]", currentStateId, callbackSource),
            currentStateId, null, null);
    }

    public StateNode getStartState(StateMachineDefinition definition) {
        if (definition == null || definition.getNodes() == null) {
            return null;
        }

        return definition.getNodes().stream()
            .filter(state -> "START".equals(state.getType()))
            .findFirst()
            .orElse(null);
    }

    public List<Transition> getAvailableTransitions(StateMachineDefinition definition, String currentStateId,
                                                     String triggerSource) {
        if (definition == null || currentStateId == null || definition.getTransitions() == null) {
            return new ArrayList<>();
        }

        return definition.getTransitions().stream()
            .filter(t -> currentStateId.equals(t.getSourceStateId()))
            .filter(t -> triggerSource == null || triggerSource.equals(t.getTriggerSource()))
            .collect(Collectors.toList());
    }

    public boolean canTransition(StateMachineDefinition definition, String currentStateId,
                                  String targetStateId, Map<String, Object> context) {
        try {
            return canTransitionInternal(definition, currentStateId, targetStateId, context);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canTransitionInternal(StateMachineDefinition definition, String currentStateId,
                                           String targetStateId, Map<String, Object> context) {
        if (definition == null || currentStateId == null || targetStateId == null) {
            return false;
        }

        if (currentStateId.equals(targetStateId)) {
            return true;
        }

        if (definition.getTransitions() == null) {
            return false;
        }

        for (Transition transition : definition.getTransitions()) {
            if (currentStateId.equals(transition.getSourceStateId())
                && targetStateId.equals(transition.getTargetStateId())) {
                String condition = transition.getCondition();
                if (condition == null || condition.trim().isEmpty()) {
                    return true;
                }
                return expressionEvaluator.evaluate(condition, context);
            }
        }

        return false;
    }

    private StateMachineDefinition loadStateMachineDefinition(Long tenantId, Long stateMachineId) {
        if (tenantId == null || stateMachineId == null) {
            return null;
        }
        return stateMachineCache.get(tenantId, stateMachineId);
    }

    private Transition findTransition(StateMachineDefinition definition, String fromStateId,
                                       String toStateId, String triggerSource) {
        if (definition.getTransitions() == null) {
            return null;
        }

        return definition.getTransitions().stream()
            .filter(t -> fromStateId.equals(t.getSourceStateId()))
            .filter(t -> toStateId.equals(t.getTargetStateId()))
            .filter(t -> triggerSource == null || triggerSource.equals(t.getTriggerSource()))
            .findFirst()
            .orElse(null);
    }

    private List<Transition> findCallbackTransitions(StateMachineDefinition definition, String currentStateId,
                                                      String callbackSource) {
        if (definition.getTransitions() == null) {
            return new ArrayList<>();
        }

        return definition.getTransitions().stream()
            .filter(t -> currentStateId.equals(t.getSourceStateId()))
            .filter(t -> TRIGGER_SOURCE_CALLBACK.equals(t.getTriggerSource()))
            .filter(t -> callbackSource == null || callbackSource.equals(t.getCallbackSource()))
            .collect(Collectors.toList());
    }

    private StateNode findStateNode(StateMachineDefinition definition, String stateId) {
        if (definition.getNodes() == null || stateId == null) {
            return null;
        }
        return definition.getNodes().stream()
            .filter(node -> stateId.equals(node.getId()))
            .findFirst()
            .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEvaluationContext(TicketInstance ticket, Map<String, Object> triggerData) {
        Map<String, Object> context = new HashMap<>();

        if (ticket != null) {
            context.put("ticketId", ticket.getId());
            context.put("tenantId", ticket.getTenantId());
            context.put("currentStateId", ticket.getCurrentStateId());
            context.put("currentStateName", ticket.getCurrentStateName());
            context.put("title", ticket.getTitle());
            context.put("businessKey", ticket.getBusinessKey());

            if (ticket.getPayload() != null && !ticket.getPayload().trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> payload = mapper.readValue(ticket.getPayload(), Map.class);
                    context.putAll(payload);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        if (triggerData != null) {
            context.putAll(triggerData);
        }

        return context;
    }

    public static class TransitionResult {
        private final boolean success;
        private final String fromState;
        private final String toState;
        private final Transition transition;
        private final String message;

        private TransitionResult(boolean success, String fromState, String toState,
                                  Transition transition, String message) {
            this.success = success;
            this.fromState = fromState;
            this.toState = toState;
            this.transition = transition;
            this.message = message;
        }

        public static TransitionResult success(String fromState, String toState, Transition transition) {
            return new TransitionResult(true, fromState, toState, transition,
                String.format("状态转移成功: [%s] -> [%s]", fromState, toState));
        }

        public static TransitionResult failure(String message) {
            return new TransitionResult(false, null, null, null, message);
        }

        public static TransitionResult failure(String message, String fromState, String toState,
                                                Transition transition) {
            return new TransitionResult(false, fromState, toState, transition, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFromState() {
            return fromState;
        }

        public String getToState() {
            return toState;
        }

        public Transition getTransition() {
            return transition;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "TransitionResult{" +
                "success=" + success +
                ", fromState='" + fromState + '\'' +
                ", toState='" + toState + '\'' +
                ", transition=" + (transition != null ? transition.getId() : "null") +
                ", message='" + message + '\'' +
                '}';
        }
    }
}
