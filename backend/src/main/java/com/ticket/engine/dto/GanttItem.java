package com.ticket.engine.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GanttItem {
    private String stateId;
    private String stateName;
    private String startTime;
    private String endTime;
    private long durationMillis;
    private double durationMinutes;
    private String durationDisplay;
    private boolean isCurrent;
    private int sequence;
}
