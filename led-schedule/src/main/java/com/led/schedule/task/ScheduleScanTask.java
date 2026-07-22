package com.led.schedule.task;

import com.led.common.entity.Schedule;
import com.led.schedule.service.ScheduleService;
import com.led.schedule.util.ScheduleLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时扫描调度任务：每15秒检查是否有到期的定时播放
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleScanTask {

    private final ScheduleService scheduleService;
    private final ScheduleLockManager scheduleLockManager;

    /** 每15秒扫描一次 */
    @Scheduled(fixedRate = 15000)
    public void scanAndExecute() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Schedule> schedules = scheduleService.getEnabledSchedules();
            for (Schedule s : schedules) {
                // 检查是否到了执行时间
                if (s.getNextRunTime() == null || s.getNextRunTime().isAfter(now)) {
                    continue;
                }
                // 加锁：确保"检查→更新DB→下发MQTT"原子化，防止与手动触发并发
                synchronized (scheduleLockManager.getLock(s.getId())) {
                    Schedule latest = scheduleService.getById(s.getId());
                    if (latest == null || latest.getNextRunTime() == null
                            || latest.getNextRunTime().isAfter(now)) {
                        continue; // 已被手动触发处理过，跳过
                    }
                    s.setLastRunTime(now);
                    scheduleService.calcNextRunTime(s);
                    if (s.getNextRunTime() == null) {
                        s.setEnabled(0);
                    }
                    scheduleService.updateById(s);
                    log.info("[定时调度] 开始执行: program={}, screen={}, nextRunTime={}",
                            s.getProgramId(), s.getScreenId(), s.getNextRunTime());
                    try {
                        scheduleService.executePlay(s);
                        log.info("[定时调度] 执行完成: id={}", s.getId());
                    } catch (Exception e) {
                        log.error("[定时调度] 执行失败: id={}, msg={}", s.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[定时调度] 扫描异常: {}", e.getMessage());
        }
    }

}
