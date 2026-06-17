package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.CallbackRequest;
import com.ticket.engine.dto.CallbackResponse;
import com.ticket.engine.dto.TransitionResponse;
import com.ticket.engine.engine.TenantContext;
import com.ticket.engine.service.TicketService;
import com.ticket.engine.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "外部回调接入", description = "接收外部系统回调的接口")
@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    @Resource
    private TicketService ticketService;

    @Resource
    private WebhookService webhookService;

    @Operation(summary = "接收外部系统回调", description = "接收外部系统的回调请求并处理工单状态转移")
    @PostMapping("/{tenantId}/{callbackSource}")
    public ResponseEntity<ApiResponse<CallbackResponse>> receiveCallback(
            @Parameter(description = "租户ID") @PathVariable Long tenantId,
            @Parameter(description = "回调来源") @PathVariable String callbackSource,
            @RequestBody CallbackRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        try {
            TenantContext.setCurrentTenantId(tenantId);
            TransitionResponse transitionResponse = ticketService.executeCallbackTransition(tenantId, callbackSource, request);
            CallbackResponse response = CallbackResponse.builder()
                    .success(transitionResponse.getSuccess())
                    .transitionTriggered(transitionResponse.getSuccess())
                    .currentState(transitionResponse.getCurrentStateId())
                    .message(transitionResponse.getMessage())
                    .build();
            return ResponseEntity.ok(ApiResponse.success(response));
        } finally {
            TenantContext.clear();
        }
    }
}
