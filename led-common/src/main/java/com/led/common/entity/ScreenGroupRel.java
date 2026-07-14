package com.led.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("t_screen_group_rel")
public class ScreenGroupRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long screenId;
}
