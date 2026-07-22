package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_screen")
public class Screen {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String ipAddress;
    private Integer resolutionWidth;
    private Integer resolutionHeight;
    private Integer brightness;
    private String status;
    private String location;
    private String model;
    private String mqttClientId;
    private LocalDateTime lastHeartbeat;
    @JsonProperty("createTime")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
