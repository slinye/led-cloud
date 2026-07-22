
package com.led.schedule.controller;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.Schedule;
import com.led.schedule.service.ScheduleService;
import com.led.schedule.util.ScheduleLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleLockManager scheduleLockManager;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 列表 */
    @GetMapping
    public R<List<Schedule>> list() {
        return R.ok(scheduleService.listAllWithDetails());
    }

    /** 创建定时 */
    @PostMapping
    @AuditLog(module = "定时调度", action = "CREATE", description = "创建定时播放")
    public R<Void> create(@RequestBody Map<String, Object> params) {
        Schedule s = new Schedule();
        s.setProgramId(Long.valueOf(params.get("programId").toString()));
        s.setScreenId(Long.valueOf(params.get("screenId").toString()));
        s.setCronExpression((String) params.get("cronExpression"));
        // 一次性：解析 startTime
        String startStr = (String) params.get("startTime");
        if (startStr != null && !startStr.isEmpty()) {
            s.setStartTime(LocalDateTime.parse(startStr, DTF));
        } else {
            s.setStartTime(LocalDateTime.now());
        }
        String endStr = (String) params.get("endTime");
        if (endStr != null && !endStr.isEmpty()) {
            s.setEndTime(LocalDateTime.parse(endStr, DTF));
        }
        s.setEnabled(1);
        s.setNextRunTime(s.getStartTime());
        scheduleService.save(s);
        return R.okMsg("定时已创建");
    }

    /** 更新 */
    @PutMapping("/{id}")
    @AuditLog(module = "定时调度", action = "UPDATE", description = "更新定时播放")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Schedule s = scheduleService.getById(id);
        if (s == null) return R.fail(404, "定时不存在");
        boolean needRecalc = false;
        if (params.containsKey("programId")) s.setProgramId(Long.valueOf(params.get("programId").toString()));
        if (params.containsKey("screenId")) s.setScreenId(Long.valueOf(params.get("screenId").toString()));
        if (params.containsKey("cronExpression")) {
            s.setCronExpression((String) params.get("cronExpression"));
            needRecalc = true;  // Cron 变了，需重新计算下次执行时间
        }
        if (params.containsKey("startTime")) {
            String startStr = (String) params.get("startTime");
            if (startStr != null && !startStr.isEmpty()) {
                s.setStartTime(LocalDateTime.parse(startStr, DTF));
                needRecalc = true;
            }
        }
        if (params.containsKey("endTime")) {
            String endStr = (String) params.get("endTime");
            if (endStr != null && !endStr.isEmpty()) {
                s.setEndTime(LocalDateTime.parse(endStr, DTF));
            } else {
                s.setEndTime(null);  // 允许清空结束时间
            }
            needRecalc = true;
        }
        if (params.containsKey("enabled")) {
            s.setEnabled(Integer.parseInt(params.get("enabled").toString()));
            needRecalc = true;
        }
        // 重新计算下次执行时间
        if (needRecalc && s.getEnabled() == 1) {
            scheduleService.calcNextRunTime(s);
        }
        scheduleService.updateById(s);
        return R.okMsg("更新成功");
    }

    /** 启用/停用 */
    @PutMapping("/{id}/toggle")
    @AuditLog(module = "定时调度", action = "TOGGLE", description = "启用/停用定时")
    public R<Void> toggle(@PathVariable Long id) {
        Schedule s = scheduleService.getById(id);
        if (s == null) return R.fail(404, "定时不存在");
        s.setEnabled(s.getEnabled() == 1 ? 0 : 1);
        if (s.getEnabled() == 1) {
            // 根据 Cron 表达式重新计算下次执行时间
            scheduleService.calcNextRunTime(s);
        } else {
            s.setNextRunTime(null);
        }
        scheduleService.updateById(s);
        return R.okMsg(s.getEnabled() == 1 ? "已启用" : "已停用");
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @AuditLog(module = "定时调度", action = "DELETE", description = "删除定时")
    public R<Void> delete(@PathVariable Long id) {
        scheduleService.removeById(id);
        return R.okMsg("已删除");
    }

    /** 手动触发一次 */
    @PostMapping("/{id}/trigger")
    @AuditLog(module = "定时调度", action = "TRIGGER", description = "手动触发定时播放")
    public R<Void> trigger(@PathVariable Long id) {
        synchronized (scheduleLockManager.getLock(id)) {
            Schedule s = scheduleService.getById(id);
            if (s == null) return R.fail(404, "定时不存在");
            // 防止扫描任务刚执行完，手动又触发一次
            if (s.getLastRunTime() != null
                    && s.getLastRunTime().plusSeconds(14).isAfter(LocalDateTime.now())) {
                return R.okMsg("定时刚刚已执行，请稍后再试");
            }
            s.setLastRunTime(LocalDateTime.now());
            scheduleService.calcNextRunTime(s);
            scheduleService.updateById(s);
            scheduleService.executePlay(s);
        }
        return R.okMsg("已触发");
    }
}
