package com.led.schedule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.PlayLog;
import com.led.schedule.mapper.PlayLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayLogService extends ServiceImpl<PlayLogMapper, PlayLog> {

    public List<PlayLog> getByScreenId(Long screenId) {
        return baseMapper.selectByScreenId(screenId);
    }

    /** 分页查询播放日志（带屏幕/节目名称和状态，支持时间范围筛选） */
    public IPage<PlayLog> pageWithDetails(int page, int size,
                                           String screenName, String programName, String status,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectPageWithDetails(
                new Page<>(page, size), screenName, programName, status, startTime, endTime);
    }

    /** 导出全部符合条件的日志（不分页） */
    public List<PlayLog> getAllForExport(String screenName, String programName, String status,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectAllForExport(screenName, programName, status, startTime, endTime);
    }
}
