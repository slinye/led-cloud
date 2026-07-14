package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("t_program_item")
public class ProgramItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long programId;
    private Long contentId;
    private Integer sortOrder;
    private Integer duration;

    @TableField(exist = false)
    private String contentName;
    @TableField(exist = false)
    private String contentType;
    @TableField(exist = false)
    private String filePath;
    @TableField(exist = false)
    private String textContent;
    @TableField(exist = false)
    private Integer fontSize;
    @TableField(exist = false)
    private String fontColor;
    @TableField(exist = false)
    private String bgColor;
    @TableField(exist = false)
    private Integer scrollSpeed;
}
