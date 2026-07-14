package com.led.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/** 微信小程序登录请求 */
@Data
public class WechatLoginRequest {
    @NotBlank(message = "微信登录code不能为空")
    private String code;

    /** 可选：微信用户昵称 */
    private String nickName;

    /** 可选：微信头像URL */
    private String avatarUrl;
}
