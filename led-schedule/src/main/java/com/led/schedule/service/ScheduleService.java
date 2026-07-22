package com.led.schedule.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.led.common.entity.Program;
import com.led.common.entity.ProgramItem;
import com.led.common.entity.Schedule;
import com.led.common.entity.Screen;
import com.led.schedule.feign.DeviceFeignClient;
import com.led.schedule.mapper.ScheduleMapper;
import com.led.schedule.mapper.ScreenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService extends ServiceImpl<ScheduleMapper, Schedule> {

    private final ScheduleMapper scheduleMapper;
    private final ProgramService programService;
    private final DeviceFeignClient deviceFeignClient;
    private final ScreenMapper screenMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 获取所有启用的调度 */
    public List<Schedule> getEnabledSchedules() {
        return scheduleMapper.selectEnabledSchedules();
    }

    /** 获取所有调度（带节目/屏幕名） */
    public List<Schedule> listAllWithDetails() {
        return scheduleMapper.selectAllWithDetails();
    }

    /** 执行定时播放：组装节目并下发 MQTT 命令 */
    public void executePlay(Schedule s) {
        try {
            Program program = programService.getWithItems(s.getProgramId());
            if (program == null || program.getItems().isEmpty()) {
                log.warn("[定时调度] 节目为空或没有素材: programId={}", s.getProgramId());
                return;
            }

            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (ProgramItem item : program.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("contentId", item.getContentId());
                itemMap.put("sortOrder", item.getSortOrder());
                itemMap.put("duration", item.getDuration());
                itemMap.put("contentName", item.getContentName());
                itemMap.put("contentType", item.getContentType());
                itemMap.put("filePath", item.getFilePath());
                itemMap.put("textContent", item.getTextContent());
                itemMap.put("fontSize", item.getFontSize());
                itemMap.put("fontColor", item.getFontColor());
                itemMap.put("bgColor", item.getBgColor());
                itemMap.put("scrollSpeed", item.getScrollSpeed());
                itemsList.add(itemMap);
            }

            String mqttClientId = s.getMqttClientId();
            if (mqttClientId == null || mqttClientId.isEmpty()) {
                Screen screen = screenMapper.selectById(s.getScreenId());
                if (screen != null) {
                    mqttClientId = screen.getMqttClientId();
                }
            }
            if (mqttClientId == null || mqttClientId.isEmpty()) {
                log.warn("[定时调度] 屏幕 {} 未注册或无 mqttClientId，跳过执行", s.getScreenId());
                return;
            }

            Map<String, Object> cmd = new HashMap<>();
            cmd.put("type", "play");
            cmd.put("mqttClientId", mqttClientId);
            cmd.put("programName", program.getName());
            cmd.put("items", objectMapper.writeValueAsString(itemsList));
            deviceFeignClient.sendMqttCommand(cmd);
            log.info("[定时调度] MQTT 命令已下发: screen={}, mqttClientId={}", s.getScreenId(), mqttClientId);
        } catch (Exception e) {
            throw new RuntimeException("定时播放发送失败: " + e.getMessage(), e);
        }
    }

    /** 计算下次执行时间（从当前时间开始，找下一次匹配的时间点） */
    public void calcNextRunTime(Schedule s) {
        LocalDateTime now = LocalDateTime.now();
        String cron = s.getCronExpression();
        if (cron == null || cron.isEmpty()) {
            // 没有 cron → 一次性任务，手动触发后停用
            s.setNextRunTime(null);
            return;
        }
        // daily:08:30 → 每天指定时间
        if (cron.startsWith("daily:")) {
            String[] hm = cron.substring(6).split(":");
            int hour = Integer.parseInt(hm[0]);
            int minute = Integer.parseInt(hm[1]);
            LocalDateTime next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            // 如果今天的时间已过，则取明天
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
            if (s.getEndTime() != null && next.isAfter(s.getEndTime())) {
                s.setNextRunTime(null);
                return;
            }
            s.setNextRunTime(next);
            return;
        }
        // weekly:MON,08:00 → 每周指定星期几
        if (cron.startsWith("weekly:")) {
            String part = cron.substring(7);
            String[] segs = part.split(",", 2);
            int targetDay = dayOfWeek(segs[0]);
            String timeStr = segs.length > 1 ? segs[1] : "00:00";
            String[] hm = timeStr.split(":");
            int hour = Integer.parseInt(hm[0]);
            int minute = Integer.parseInt(hm[1]);
            // 从今天开始找下一个匹配的星期
            LocalDateTime next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            for (int i = 0; i < 7; i++) {
                if (next.getDayOfWeek().getValue() == targetDay && next.isAfter(now)) {
                    break;
                }
                next = next.plusDays(1);
            }
            if (s.getEndTime() != null && next.isAfter(s.getEndTime())) {
                s.setNextRunTime(null);
                return;
            }
            s.setNextRunTime(next);
            return;
        }
        // hourly:N → 每N小时
        if (cron.startsWith("hourly:")) {
            int hours = Integer.parseInt(cron.substring(7));
            LocalDateTime next = now.plusHours(hours);
            if (s.getEndTime() != null && next.isAfter(s.getEndTime())) {
                s.setNextRunTime(null);
                return;
            }
            s.setNextRunTime(next);
            return;
        }
        // 标准 Quartz Cron 表达式（6 或 7 域），例如 0 0 18 * * ?
        try {
            CronExpression cronExpression = new CronExpression(cron);
            cronExpression.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            Date nextDate = cronExpression.getNextValidTimeAfter(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
            if (nextDate == null) {
                s.setNextRunTime(null);
                return;
            }
            LocalDateTime next = nextDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            if (s.getEndTime() != null && next.isAfter(s.getEndTime())) {
                s.setNextRunTime(null);
                return;
            }
            s.setNextRunTime(next);
        } catch (ParseException e) {
            log.warn("[定时调度] Cron 表达式解析失败: cron={}, error={}", cron, e.getMessage());
            s.setNextRunTime(null);
        }
    }

    private int dayOfWeek(String abbr) {
        switch (abbr.toUpperCase()) {
            case "MON": return 1;
            case "TUE": return 2;
            case "WED": return 3;
            case "THU": return 4;
            case "FRI": return 5;
            case "SAT": return 6;
            case "SUN": return 7;
            default: return 1;
        }
    }
}
