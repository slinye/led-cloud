package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_program_publish")
public class ProgramPublish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long programId;
    private Integer version;
    private String snapshot;
    private String operator;
    private String remark;

    /** 发布类型: create（创建发布）/ edit（编辑发布）/ play（播放发布） */
    private String publishType;

    /** 目标屏幕/分组快照，JSON 数组 */
    private String targetScreens;

    /** 发布结果: success / fail */
    private String result;

    @JsonProperty("createTime")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
