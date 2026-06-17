package com.ticket.engine.service;

import com.ticket.engine.dto.*;
import com.ticket.engine.engine.ExpressionEvaluator;
import com.ticket.engine.engine.StateMachineCache;
import com.ticket.engine.engine.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimulationService {

    @Autowired
    private StateMachineService stateMachineService;

    @Autowired
    private ExpressionEvaluator expressionEvaluator;

    @Autowired
    private StateMachineCache stateMachineCache;

    @Transactional(readOnly = true)
    public SimulationResult simulate(SimulationRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }
        if (request.getStateMachineId() == null) {
            throw new RuntimeException("状态机ID不能为空");
        }
        if (request.getCurrentStateId() == null) {
            throw new RuntimeException("当前状态ID不能为空");
        }

        StateMachineDefinition definition = stateMachineService.getStateMachineDefinition(request.getStateMachineId());

        Map<String, String> stateNameMap = definition.getNodes().stream()
                .collect(Collectors.toMap(StateNode::getId, StateNode::getName, (a, b) -> a));

        String currentStateName = stateNameMap.getOrDefault(request.getCurrentStateId(), request.getCurrentStateId());

        List<Transition> transitions = definition.getTransitions();
        if (transitions == null) {
            transitions = Collections.emptyList();
        }

        List<Transition> matchedTransitions = transitions.stream()
                .filter(t -> request.getCurrentStateId().equals(t.getSourceStateId()))
                .filter(t -> request.getTriggerSource() == null || request.getTriggerSource().equals(t.getTriggerSource()))
                .filter(t -> request.getCallbackSource() == null || request.getCallbackSource().equals(t.getCallbackSource()))
                .collect(Collectors.toList());

        Map<String, Object> context = request.getTriggerData() != null ? request.getTriggerData() : Collections.emptyMap();

        List<SimulationPath> possiblePaths = new ArrayList<>();
        List<SimulationPath> matchedPaths = new ArrayList<>();
        Set<String> finalStateIds = new LinkedHashSet<>();

        for (Transition transition : matchedTransitions) {
            boolean conditionMet = expressionEvaluator.evaluateSafe(transition.getCondition(), context);
            String conditionResult = conditionMet ? "true" : "false";

            SimulationPath path = SimulationPath.builder()
                    .transitionId(transition.getId())
                    .transitionName(transition.getName())
                    .fromStateId(transition.getSourceStateId())
                    .fromStateName(stateNameMap.getOrDefault(transition.getSourceStateId(), transition.getSourceStateId()))
                    .toStateId(transition.getTargetStateId())
                    .toStateName(stateNameMap.getOrDefault(transition.getTargetStateId(), transition.getTargetStateId()))
                    .condition(transition.getCondition())
                    .conditionResult(conditionResult)
                    .conditionMet(conditionMet)
                    .triggerSource(transition.getTriggerSource())
                    .callbackSource(transition.getCallbackSource())
                    .build();

            possiblePaths.add(path);

            if (conditionMet) {
                matchedPaths.add(path);
                finalStateIds.add(transition.getTargetStateId());
            }
        }

        return SimulationResult.builder()
                .currentStateId(request.getCurrentStateId())
                .currentStateName(currentStateName)
                .possiblePaths(possiblePaths)
                .matchedPaths(matchedPaths)
                .finalStates(new ArrayList<>(finalStateIds))
                .triggerSource(request.getTriggerSource())
                .callbackSource(request.getCallbackSource())
                .build();
    }
}
