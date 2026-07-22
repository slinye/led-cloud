package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_play_log")
public class PlayLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long screenId;
    private Long programId;
    private String action;
    private String message;

    @JsonProperty("createTime")
    private LocalDateTime createdAt;

    /** 关联查询的屏幕名称（不入库） */
    @TableField(exist = false)
    private String screenName;

    /** 关联查询的节目名称（不入库） */
    @TableField(exist = false)
    private String programName;

    /** 播放状态: success / fail（不入库，由 action 推导） */
    @TableField(exist = false)
    private String status;
}
