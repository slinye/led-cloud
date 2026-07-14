package com.led.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.Screen;
import com.led.device.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    /** 分页查询屏幕列表 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Page<Screen>> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String status) {
        return R.ok(screenService.pageScreens(page, size, keyword, status));
    }

    /** 查询全部屏幕 */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<List<Screen>> all() {
        return R.ok(screenService.list());
    }

    /** 查询单个屏幕 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Screen> getById(@PathVariable Long id) {
        return R.ok(screenService.getById(id));
    }

    /** 新增屏幕 */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "设备管理", action = "CREATE", description = "新增屏幕")
    public R<Void> create(@RequestBody Screen screen) {
        screenService.createScreen(screen);
        return R.okMsg("新增成功");
    }

    /** 编辑屏幕 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @AuditLog(module = "设备管理", action = "UPDATE", description = "编辑屏幕")
    public R<Void> update(@PathVariable Long id, @RequestBody Screen screen) {
        screen.setId(id);
        screenService.updateScreen(screen);
        return R.okMsg("更新成功");
    }

    /** 删除屏幕 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "设备管理", action = "DELETE", description = "删除屏幕")
    public R<Void> delete(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return R.okMsg("删除成功");
    }
}
