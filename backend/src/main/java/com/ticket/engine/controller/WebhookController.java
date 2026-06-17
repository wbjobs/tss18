package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.dto.WebhookConfigRequest;
import com.ticket.engine.entity.WebhookConfig;
import com.ticket.engine.entity.WebhookLog;
import com.ticket.engine.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Webhook配置", description = "Webhook配置的增删改查、测试和日志查询接口")
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Resource
    private WebhookService webhookService;

    @Operation(summary = "创建Webhook配置", description = "创建新的Webhook配置")
    @PostMapping
    public ResponseEntity<ApiResponse<WebhookConfig>> createWebhook(
            @RequestBody WebhookConfigRequest request) {
        WebhookConfig webhook = webhookService.createWebhookConfig(request);
        return ResponseEntity.ok(ApiResponse.success(webhook));
    }

    @Operation(summary = "更新Webhook配置", description = "更新Webhook配置信息")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WebhookConfig>> updateWebhook(
            @Parameter(description = "Webhook配置ID") @PathVariable Long id,
            @RequestBody WebhookConfigRequest request) {
        WebhookConfig webhook = webhookService.updateWebhookConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success(webhook));
    }

    @Operation(summary = "删除Webhook配置", description = "删除指定的Webhook配置")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(
            @Parameter(description = "Webhook配置ID") @PathVariable Long id) {
        webhookService.deleteWebhookConfig(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "获取Webhook配置详情", description = "根据ID获取Webhook配置详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WebhookConfig>> getWebhook(
            @Parameter(description = "Webhook配置ID") @PathVariable Long id) {
        WebhookConfig webhook = webhookService.getWebhookConfig(id);
        return ResponseEntity.ok(ApiResponse.success(webhook));
    }

    @Operation(summary = "分页查询Webhook配置列表", description = "分页查询Webhook配置列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<WebhookConfig>>> listWebhooks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        PageResult<WebhookConfig> result = webhookService.listWebhookConfigs(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "测试Webhook调用", description = "测试Webhook配置是否能正常调用")
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testWebhook(
            @Parameter(description = "Webhook配置ID") @PathVariable Long id) {
        boolean success = webhookService.testWebhook(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "Webhook调用成功" : "Webhook调用失败");
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "查询Webhook调用日志", description = "查询Webhook的调用日志列表")
    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<PageResult<WebhookLog>>> getWebhookLogs(
            @Parameter(description = "Webhook配置ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        PageResult<WebhookLog> logs = webhookService.listWebhookLogs(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
