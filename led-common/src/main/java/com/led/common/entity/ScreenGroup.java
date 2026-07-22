package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_screen_group")
public class ScreenGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String area;
    @JsonProperty("createTime")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
