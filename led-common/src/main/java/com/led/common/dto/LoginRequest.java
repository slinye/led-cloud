package com.led.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/** 登录请求 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 验证码Key（可选，为空则跳过验证码校验） */
    private String captchaKey;
    /** 用户输入的验证码 */
    private String captchaCode;
}
