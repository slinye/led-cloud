package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long programId;
    private Long screenId;
    private String cronExpression;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer enabled;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;

    @JsonProperty("createTime")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 关联查询：节目名称 */
    @TableField(exist = false)
    private String programName;
    /** 关联查询：屏幕名称 */
    @TableField(exist = false)
    private String screenName;
    /** 关联查询：屏幕MQTT ClientId */
    @TableField(exist = false)
    private String mqttClientId;
}
