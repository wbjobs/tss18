package com.ticket.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackRequest {

    private String businessKey;

    private String eventType;

    private Map<String, Object> data;

    private LocalDateTime timestamp;
}
