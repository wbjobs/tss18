package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.CreateStateMachineRequest;
import com.ticket.engine.dto.PageResult;
import com.ticket.engine.dto.UpdateStateMachineRequest;
import com.ticket.engine.entity.StateMachineDef;
import com.ticket.engine.service.StateMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "状态机管理", description = "状态机的创建、更新、发布、下线等接口")
@RestController
@RequestMapping("/api/state-machine")
public class StateMachineController {

    @Resource
    private StateMachineService stateMachineService;

    @Operation(summary = "创建状态机", description = "创建新的状态机定义")
    @PostMapping
    public ResponseEntity<ApiResponse<StateMachineDef>> createStateMachine(
            @RequestBody CreateStateMachineRequest request) {
        StateMachineDef stateMachine = stateMachineService.createStateMachine(request);
        return ResponseEntity.ok(ApiResponse.success(stateMachine));
    }

    @Operation(summary = "更新状态机定义", description = "更新状态机的节点和转移规则")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StateMachineDef>> updateStateMachine(
            @Parameter(description = "状态机ID") @PathVariable Long id,
            @RequestBody UpdateStateMachineRequest request) {
        StateMachineDef stateMachine = stateMachineService.updateStateMachine(id, request);
        return ResponseEntity.ok(ApiResponse.success(stateMachine));
    }

    @Operation(summary = "发布状态机", description = "发布状态机，使其生效")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<StateMachineDef>> publishStateMachine(
            @Parameter(description = "状态机ID") @PathVariable Long id) {
        StateMachineDef stateMachine = stateMachineService.publishStateMachine(id);
        return ResponseEntity.ok(ApiResponse.success(stateMachine));
    }

    @Operation(summary = "下线状态机", description = "下线状态机，使其失效")
    @PostMapping("/{id}/offline")
    public ResponseEntity<ApiResponse<StateMachineDef>> offlineStateMachine(
            @Parameter(description = "状态机ID") @PathVariable Long id) {
        StateMachineDef stateMachine = stateMachineService.offlineStateMachine(id);
        return ResponseEntity.ok(ApiResponse.success(stateMachine));
    }

    @Operation(summary = "获取状态机详情", description = "根据ID获取状态机详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StateMachineDef>> getStateMachine(
            @Parameter(description = "状态机ID") @PathVariable Long id) {
        StateMachineDef stateMachine = stateMachineService.getStateMachine(id);
        return ResponseEntity.ok(ApiResponse.success(stateMachine));
    }

    @Operation(summary = "分页查询状态机列表", description = "分页查询状态机列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<StateMachineDef>>> listStateMachines(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        PageResult<StateMachineDef> result = stateMachineService.listStateMachines(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "删除状态机", description = "删除指定的状态机")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStateMachine(
            @Parameter(description = "状态机ID") @PathVariable Long id) {
        stateMachineService.deleteStateMachine(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
