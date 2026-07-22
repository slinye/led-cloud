package com.led.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.auth.service.AuthService;
import com.led.auth.service.UserService;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /** 分页查询用户 */
    @GetMapping
    public R<Page<User>> list(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String keyword) {
        return R.ok(userService.pageUsers(page, size, keyword));
    }

    /** 创建用户 */
    @PostMapping
    @AuditLog(module = "用户管理", action = "CREATE", description = "创建用户")
    public R<Void> create(@RequestBody User user) {
        userService.createUser(user);
        return R.okMsg("创建成功");
    }

    /** 编辑用户信息（角色/昵称等，不含密码） */
    @PutMapping("/{id}")
    @AuditLog(module = "用户管理", action = "UPDATE", description = "编辑用户")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        userService.updateUser(id, params);
        return R.okMsg("更新成功");
    }

    /** 重置用户密码 */
    @PutMapping("/{id}/reset-password")
    @AuditLog(module = "用户管理", action = "RESET_PWD", description = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String newPassword = params.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return R.fail(400, "密码至少6位");
        }
        userService.resetPassword(id, newPassword);
        return R.okMsg("密码已重置");
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    @AuditLog(module = "用户管理", action = "DELETE", description = "删除用户")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.okMsg("删除成功");
    }

    /** 获取可选角色列表 */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<List<Map<String, String>>> roles() {
        return R.ok(authService.getRoles());
    }
}
