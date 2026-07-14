package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private LocalDateTime createdAt;
}
