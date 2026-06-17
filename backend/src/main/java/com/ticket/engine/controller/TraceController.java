package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.entity.StateTransitionTrace;
import com.ticket.engine.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "轨迹查询", description = "工单状态转移轨迹查询接口")
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    @Resource
    private TraceService traceService;

    @Operation(summary = "查询工单轨迹列表", description = "根据工单ID查询状态转移轨迹列表")
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ApiResponse<List<StateTransitionTrace>>> getTicketTraces(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        List<StateTransitionTrace> traces = traceService.getTicketTraces(ticketId);
        return ResponseEntity.ok(ApiResponse.success(traces));
    }

    @Operation(summary = "分页查询所有轨迹", description = "分页查询所有状态转移轨迹")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<StateTransitionTrace>>> listTraces(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        PageResult<StateTransitionTrace> result = traceService.listTraces(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
