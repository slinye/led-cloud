package com.led.auth.controller;

import com.led.auth.service.AuthService;
import com.led.common.dto.R;
import com.led.common.dto.WechatLoginRequest;
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
}
