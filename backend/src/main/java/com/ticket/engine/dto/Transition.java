package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transition {

    private String id;

    private String name;

    private String sourceStateId;

    private String targetStateId;

    private String condition;

    private String triggerSource;

    private String callbackSource;
}
