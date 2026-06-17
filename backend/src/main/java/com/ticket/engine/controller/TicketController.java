package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.CreateTicketRequest;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.dto.TransitionRequest;
import com.ticket.engine.dto.TransitionResponse;
import com.ticket.engine.entity.TicketInstance;
import com.ticket.engine.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "工单管理", description = "工单的创建、查询、状态转移等接口")
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    @Operation(summary = "创建工单", description = "创建新的工单实例")
    @PostMapping
    public ResponseEntity<ApiResponse<TicketInstance>> createTicket(
            @RequestBody CreateTicketRequest request) {
        TicketInstance ticket = ticketService.createTicket(request);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @Operation(summary = "获取工单详情", description = "根据ID获取工单详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketInstance>> getTicket(
            @Parameter(description = "工单ID") @PathVariable Long id) {
        TicketInstance ticket = ticketService.getTicket(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @Operation(summary = "分页查询工单列表", description = "分页查询工单列表，可按状态过滤")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<TicketInstance>>> listTickets(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "状态（可选）") @RequestParam(required = false) String state) {
        PageResult<TicketInstance> result = ticketService.listTickets(page, size, state);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "执行状态转移", description = "执行工单状态转移")
    @PostMapping("/{id}/transition")
    public ResponseEntity<ApiResponse<TransitionResponse>> executeTransition(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody TransitionRequest request) {
        TransitionResponse response = ticketService.executeTransition(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
