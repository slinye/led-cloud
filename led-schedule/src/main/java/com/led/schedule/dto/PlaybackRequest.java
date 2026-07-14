package com.led.schedule.dto;

import lombok.Data;
import java.util.List;

/** 播放控制请求 */
@Data
public class PlaybackRequest {
    private List<Long> screenIds;
    private Integer brightness;
}
