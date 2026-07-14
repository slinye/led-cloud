package com.led.auth.controller;

import com.led.auth.service.AuthService;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.ChangePasswordRequest;
import com.led.common.dto.LoginRequest;
import com.led.common.dto.R;
import com.led.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户登录 */
    @PostMapping("/login")
    @AuditLog(module = "认证中心", action = "LOGIN", description = "用户登录")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = authService.login(request);
        return R.ok("登录成功", result);
    }

    /** 验证 Token */
    @GetMapping("/verify")
    public R<Map<String, Object>> verify(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return R.fail(401, "未提供Token");
        }
        String token = authHeader.substring(7);
        Map<String, Object> result = authService.verify(token);
        return R.ok(result);
    }

    /** 修改密码 */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "认证中心", action = "CHANGE_PWD", description = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                  @RequestHeader("Authorization") String authHeader) {
        Long userId = JwtUtil.getUserId(authHeader.substring(7));
        authService.changePassword(userId, request);
        return R.okMsg("密码修改成功");
    }
}
