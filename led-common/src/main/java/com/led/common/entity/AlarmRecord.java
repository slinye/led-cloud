package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_alarm_record")
public class AlarmRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 告警类型: offline / play_fail / disk */
    private String alarmType;

    /** 关联屏幕ID */
    private Long screenId;

    /** 屏幕名称（冗余字段，便于展示） */
    private String screenName;

    /** 告警标题 */
    private String title;

    /** 告警详情 */
    private String message;

    /** 状态: unread / read */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
