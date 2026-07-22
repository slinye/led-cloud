package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_content")
public class Content {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String filePath;
    private Long fileSize;
    private String thumbnailPath;
    private String textContent;
    private Integer fontSize;
    private String fontColor;
    private String bgColor;
    private Integer scrollSpeed;
    private Integer duration;
    @JsonProperty("createTime")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
