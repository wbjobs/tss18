package com.ticket.engine.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationPath {
    private String transitionId;
    private String transitionName;
    private String fromStateId;
    private String fromStateName;
    private String toStateId;
    private String toStateName;
    private String condition;
    private String conditionResult;
    private boolean conditionMet;
    private String triggerSource;
    private String callbackSource;
}
