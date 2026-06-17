package com.ticket.engine.controller;

import com.ticket.engine.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证接口", description = "用户登录和登出接口（简化版）")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String FIXED_TOKEN = "ticket-engine-fixed-token-2024";

    @Operation(summary = "登录", description = "用户登录，返回固定token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login() {
        Map<String, Object> data = new HashMap<>();
        data.put("token", FIXED_TOKEN);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", 86400);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "登出", description = "用户登出")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success());
    }
}
