package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackResponse {

    private Boolean success;

    private Boolean transitionTriggered;

    private String currentState;

    private String message;
}
