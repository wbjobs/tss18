package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import com.ticket.engine.dto.SimulationRequest;
import com.ticket.engine.dto.SimulationResult;
import com.ticket.engine.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "仿真沙箱")
public class SimulationController {

    @Resource
    private SimulationService simulationService;

    @Operation(summary = "执行仿真", description = "根据当前状态和触发源，仿真预览所有可能的转移路径")
    @PostMapping
    public ResponseEntity<ApiResponse<SimulationResult>> simulate(@RequestBody SimulationRequest request) {
        SimulationResult result = simulationService.simulate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
