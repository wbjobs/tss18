package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateMachineDefinition {

    private List<StateNode> nodes;

    private List<Transition> transitions;
}
