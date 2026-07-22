package com.led.schedule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.Program;
import com.led.common.entity.ProgramItem;
import com.led.common.entity.ProgramPublish;
import com.led.schedule.service.ProgramPublishService;
import com.led.schedule.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final ProgramPublishService programPublishService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Page<Program>> list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(programService.pagePrograms(page, size, keyword));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<List<Program>> all() {
        return R.ok(programService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Program> getById(@PathVariable Long id) {
        return R.ok(programService.getWithItems(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "节目管理", action = "CREATE", description = "创建节目")
    public R<Void> create(@RequestBody Map<String, Object> params) {
        Program program = parseProgram(params);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) params.get("items");
        List<ProgramItem> items = parseItems(itemMaps);
        programService.createProgram(program, items);
        return R.okMsg("创建成功");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "节目管理", action = "UPDATE", description = "编辑节目")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Program program = parseProgram(params);
        program.setId(id);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) params.get("items");
        List<ProgramItem> items = parseItems(itemMaps);
        String operator = getCurrentUsername();
        programService.updateProgram(program, items, operator);
        return R.okMsg("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "节目管理", action = "DELETE", description = "删除节目")
    public R<Void> delete(@PathVariable Long id) {
        programService.deleteProgram(id);
        return R.okMsg("删除成功");
    }

    /** 批量删除 */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "节目管理", action = "BATCH_DELETE", description = "批量删除节目")
    public R<Void> batchDelete(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) params.get("ids");
        if (ids != null) {
            for (Integer id : ids) {
                programService.deleteProgram(id.longValue());
            }
        }
        return R.okMsg("批量删除成功");
    }

    @GetMapping("/{id}/publishHistory")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<List<ProgramPublish>> publishHistory(@PathVariable Long id) {
        return R.ok(programPublishService.getByProgramId(id));
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal != null ? principal.toString() : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Program parseProgram(Map<String, Object> params) {
        Program p = new Program();
        p.setName((String) params.get("name"));
        p.setDescription((String) params.get("description"));
        p.setStatus((String) params.getOrDefault("status", "draft"));
        p.setScheduleType((String) params.get("scheduleType"));
        // 解析定时播放时间
        Object st = params.get("scheduleTime");
        if (st != null && !st.toString().isEmpty()) {
            try {
                p.setScheduleTime(LocalDateTime.parse(st.toString(), DTF));
            } catch (Exception ignored) {
                try { p.setScheduleTime(LocalDateTime.parse(st.toString())); } catch (Exception ignored2) {}
            }
        }
        Object et = params.get("scheduleEndTime");
        if (et != null && !et.toString().isEmpty()) {
            try {
                p.setScheduleEndTime(LocalDateTime.parse(et.toString(), DTF));
            } catch (Exception ignored) {
                try { p.setScheduleEndTime(LocalDateTime.parse(et.toString())); } catch (Exception ignored2) {}
            }
        }
        return p;
    }

    private List<ProgramItem> parseItems(List<Map<String, Object>> itemMaps) {
        if (itemMaps == null) return null;
        java.util.ArrayList<ProgramItem> items = new java.util.ArrayList<>();
        for (Map<String, Object> im : itemMaps) {
            ProgramItem item = new ProgramItem();
            Object cid = im.get("contentId");
            if (cid != null) item.setContentId(Long.valueOf(cid.toString()));
            Object sort = im.get("sortOrder");
            if (sort != null) item.setSortOrder(Integer.parseInt(sort.toString()));
            Object dur = im.get("duration");
            if (dur != null) item.setDuration(Integer.parseInt(dur.toString()));
            items.add(item);
        }
        return items;
    }
}
