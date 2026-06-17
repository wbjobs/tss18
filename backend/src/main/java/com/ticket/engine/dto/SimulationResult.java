package com.ticket.engine.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {
    private String currentStateId;
    private String currentStateName;
    private List<SimulationPath> possiblePaths;
    private List<SimulationPath> matchedPaths;
    private List<String> finalStates;
    private String triggerSource;
    private String callbackSource;
}
