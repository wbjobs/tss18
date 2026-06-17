package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.GanttItem;
import com.ticket.engine.service.GanttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/gantt")
@Tag(name = "Gantt图")
public class GanttController {

    @Resource
    private GanttService ganttService;

    @Operation(summary = "获取工单Gantt图数据", description = "根据工单轨迹记录计算Gantt图时间线数据")
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ApiResponse<List<GanttItem>>> getGanttData(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        List<GanttItem> items = ganttService.getGanttData(ticketId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
