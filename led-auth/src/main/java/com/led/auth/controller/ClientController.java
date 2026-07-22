package com.led.auth.controller;

import com.led.auth.service.AuthService;
import com.led.common.dto.BindAccountRequest;
import com.led.common.dto.R;
import com.led.common.dto.WechatLoginRequest;
import com.led.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 移动端/小程序 API（无需网关认证）
 * 路径前缀 /api/client/ 已在网关白名单中
 */
@Slf4j
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final AuthService authService;

    /** 微信小程序登录 */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody WechatLoginRequest request) {
        log.info("微信小程序登录请求, nickname={}", request.getNickName());
        Map<String, Object> result = authService.loginByWechat(request);
        return R.ok("登录成功", result);
    }

    /**
     * 微信账号绑定PC管理号
     * - 已登录的微信用户输入PC端用户名密码，将openid迁移到PC账号
     * - 绑定后以PC账号的角色身份重新签发Token
     */
    @PostMapping("/bind")
    public R<Map<String, Object>> bindAccount(@Valid @RequestBody BindAccountRequest request,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return R.fail(401, "请先微信登录后再绑定");
        }
        Long userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId == null) {
            return R.fail(401, "登录状态无效，请重新登录");
        }
        log.info("微信绑定PC账号请求: userId={}, pcUsername={}", userId, request.getUsername());
        Map<String, Object> result = authService.bindWechatAccount(userId, request);
        return R.ok("绑定成功", result);
    }
}
