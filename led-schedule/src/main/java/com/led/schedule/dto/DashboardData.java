package com.led.schedule.dto;

import lombok.Data;
import lombok.ToString;
import com.led.common.entity.Program;
import java.util.List;

/** 仪表盘数据 DTO */
@Data
public class DashboardData {
    private int totalScreens;
    private int onlineScreens;
    private int offlineScreens;
    private int totalContents;
    private int totalPrograms;
    private int totalPlayLogsToday;
    private String onlineRate;
    private List<Program> recentPrograms;
}
