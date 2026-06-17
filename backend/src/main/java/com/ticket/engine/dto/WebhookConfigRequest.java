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
public class WebhookConfigRequest {

    private String name;

    private String url;

    private String secretKey;

    private List<String> events;

    private Boolean enabled;

    private Integer retryCount;
}
