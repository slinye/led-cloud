package com.led.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.led.common.entity.Content;
import com.led.common.entity.Program;
import com.led.common.entity.Screen;
import com.led.schedule.mapper.ProgramMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 仪表盘数据聚合 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProgramMapper programMapper;

    /** 构建仪表盘摘要数据 - 供PDF导出和看板使用 */
    public Map<String, Object> getSummaryData(int totalScreens, int onlineScreens,
                                               int totalContents, int totalPrograms,
                                               int totalPlayLogsToday) {
        List<Program> programs = programMapper.selectList(
                new LambdaQueryWrapper<Program>().orderByDesc(Program::getUpdatedAt).last("LIMIT 5"));

        Map<String, Object> data = new HashMap<>();
        data.put("totalScreens", totalScreens);
        data.put("onlineScreens", onlineScreens);
        data.put("offlineScreens", totalScreens - onlineScreens);
        data.put("totalContents", totalContents);
        data.put("totalPrograms", totalPrograms);
        data.put("totalPlayLogsToday", totalPlayLogsToday);
        data.put("onlineRate", totalScreens > 0 ?
                String.format("%.1f%%", onlineScreens * 100.0 / totalScreens) : "0%");
        data.put("recentPrograms", programs);
        return data;
    }
}
