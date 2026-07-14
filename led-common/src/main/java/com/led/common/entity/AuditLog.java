package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String description;
    private String ip;
    private Long costTime;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private LocalDateTime createdAt;
}
