package com.led.schedule.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.PlayLog;
import com.led.schedule.mapper.PlayLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayLogService extends ServiceImpl<PlayLogMapper, PlayLog> {

    public List<PlayLog> getByScreenId(Long screenId) {
        return baseMapper.selectByScreenId(screenId);
    }
}
