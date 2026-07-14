package com.led.device.controller;

import com.led.common.dto.R;
import com.led.common.entity.ScreenGroup;
import com.led.common.entity.ScreenGroupRel;
import com.led.device.service.ScreenGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/screen-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ScreenGroupController {

    private final ScreenGroupService screenGroupService;

    @GetMapping
    public R<List<ScreenGroup>> list() {
        return R.ok(screenGroupService.listAll());
    }

    @PostMapping
    public R<Void> create(@RequestBody ScreenGroup group) {
        screenGroupService.createGroup(group);
        return R.okMsg("创建成功");
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ScreenGroup group) {
        group.setId(id);
        screenGroupService.updateGroup(group);
        return R.okMsg("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        screenGroupService.deleteGroup(id);
        return R.okMsg("删除成功");
    }

    /** 向分组中添加屏幕 */
    @PostMapping("/{groupId}/screens")
    public R<Void> addScreens(@PathVariable Long groupId, @RequestBody Map<String, List<Long>> body) {
        screenGroupService.addScreens(groupId, body.get("screenIds"));
        return R.okMsg("添加成功");
    }

    /** 获取分组下的屏幕列表 */
    @GetMapping("/{groupId}/screens")
    public R<List<ScreenGroupRel>> getGroupScreens(@PathVariable Long groupId) {
        return R.ok(screenGroupService.getGroupScreens(groupId));
    }
}
