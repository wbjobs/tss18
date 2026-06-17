package com.ticket.engine.service;

import com.ticket.engine.dto.GanttItem;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.entity.StateTransitionTrace;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.repository.TicketInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class GanttService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TraceService traceService;

    @Autowired
    private TicketInstanceRepository ticketInstanceRepository;

    @Transactional(readOnly = true)
    public List<GanttItem> getGanttData(Long ticketId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息不存在");
        }
        if (ticketId == null) {
            throw new RuntimeException("工单ID不能为空");
        }

        TicketInstance ticket = ticketInstanceRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new RuntimeException("工单不存在"));

        List<StateTransitionTrace> traces = traceService.getTicketTraces(ticketId);

        List<GanttItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (traces.isEmpty()) {
            GanttItem item = buildItem(
                    ticket.getCurrentStateId(),
                    ticket.getCurrentStateName(),
                    ticket.getCreatedAt(),
                    now,
                    true,
                    0
            );
            items.add(item);
            return items;
        }

        String firstStateId = traces.get(0).getFromStateId();
        String firstStateName = traces.get(0).getFromStateName();
        items.add(buildItem(firstStateId, firstStateName, ticket.getCreatedAt(), traces.get(0).getCreatedAt(), false, 0));

        for (int i = 0; i < traces.size(); i++) {
            StateTransitionTrace trace = traces.get(i);
            String stateId = trace.getToStateId();
            String stateName = trace.getToStateName();
            LocalDateTime startTime = trace.getCreatedAt();
            boolean isCurrent = (i == traces.size() - 1);
            LocalDateTime endTime = isCurrent ? now : traces.get(i + 1).getCreatedAt();

            items.add(buildItem(stateId, stateName, startTime, endTime, isCurrent, i + 1));
        }

        return items;
    }

    private GanttItem buildItem(String stateId, String stateName, LocalDateTime startTime,
                                LocalDateTime endTime, boolean isCurrent, int sequence) {
        long durationMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                - startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double durationMinutes = durationMillis / 60000.0;

        return GanttItem.builder()
                .stateId(stateId)
                .stateName(stateName)
                .startTime(startTime.format(FORMATTER))
                .endTime(isCurrent ? null : endTime.format(FORMATTER))
                .durationMillis(durationMillis)
                .durationMinutes(durationMinutes)
                .durationDisplay(formatDuration(durationMillis))
                .isCurrent(isCurrent)
                .sequence(sequence)
                .build();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        if (sb.length() == 0) {
            sb.append("不到1分钟");
        }
        return sb.toString();
    }
}
