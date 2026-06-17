package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionRequest {

    private String ticketId;

    private String targetStateId;

    private String remark;

    private Map<String, Object> triggerData;
}
