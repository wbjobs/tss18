package com.ticket.engine.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {
    private Long stateMachineId;
    private String currentStateId;
    private String triggerSource;
    private String callbackSource;
    private Map<String, Object> triggerData;
}
