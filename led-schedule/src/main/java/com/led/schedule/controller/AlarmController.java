package com.led.schedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.dto.R;
import com.led.common.entity.AlarmRecord;
import com.led.schedule.mapper.AlarmRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 告警管理接口 */
@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmRecordMapper alarmRecordMapper;

    /** 分页查询告警记录 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String status) {

        LambdaQueryWrapper<AlarmRecord> qw = new LambdaQueryWrapper<AlarmRecord>()
                .eq(alarmType != null && !alarmType.isEmpty(), AlarmRecord::getAlarmType, alarmType)
                .eq(status != null && !status.isEmpty(), AlarmRecord::getStatus, status)
                .orderByDesc(AlarmRecord::getCreatedAt);

        IPage<AlarmRecord> result = alarmRecordMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        return R.ok(map);
    }

    /** 获取未读告警数量 */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<Map<String, Long>> unreadCount() {
        long count = alarmRecordMapper.countUnread();
        Map<String, Long> map = new HashMap<>();
        map.put("unreadCount", count);
        return R.ok(map);
    }

    /** 获取最新N条告警 */
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<List<AlarmRecord>> latest(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(alarmRecordMapper.selectLatest(limit));
    }

    /** 标记单条告警已读 */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<Void> markRead(@PathVariable Long id) {
        AlarmRecord record = new AlarmRecord();
        record.setId(id);
        record.setStatus("read");
        alarmRecordMapper.updateById(record);
        return R.okMsg("已标记为已读");
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<Void> markAllRead() {
        List<AlarmRecord> unreadList = alarmRecordMapper.selectList(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, "unread"));
        for (AlarmRecord item : unreadList) {
            item.setStatus("read");
            alarmRecordMapper.updateById(item);
        }
        return R.okMsg("已全部标记为已读");
    }
}
