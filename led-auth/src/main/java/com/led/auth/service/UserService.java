package com.led.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.auth.mapper.UserMapper;
import com.led.common.entity.User;
import com.led.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

    /** 内置超级管理员用户名 */
    private static final String SUPER_ADMIN = "admin";

    /** 获取当前登录用户名 */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** 检查当前用户是否有权限操作目标用户 */
    private void checkSuperAdminProtection(User targetUser, String action) {
        if (SUPER_ADMIN.equals(targetUser.getUsername()) && !SUPER_ADMIN.equals(getCurrentUsername())) {
            throw new BusinessException(403, "无权" + action + "超级管理员");
        }
    }

    public Page<User> pageUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or()
                    .like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void createUser(User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (!Arrays.asList("ADMIN", "OPERATOR", "VIEWER").contains(user.getRole())) {
            throw new BusinessException(400, "无效的角色");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        save(user);
    }

    @Transactional
    public void updateUser(Long id, Map<String, Object> params) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 非admin用户无权修改超级管理员的信息
        checkSuperAdminProtection(user, "修改");
        // 只能编辑角色和昵称
        if (params.containsKey("role")) {
            String role = (String) params.get("role");
            if (!Arrays.asList("ADMIN", "OPERATOR", "VIEWER").contains(role)) {
                throw new BusinessException(400, "无效的角色");
            }
            // 不能把自己改成非admin
            if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)) {
                long adminCount = count(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
                if (adminCount <= 1) {
                    throw new BusinessException(400, "不能移除最后一个超级管理员的权限");
                }
            }
            // 禁止将超级管理员降级
            if (SUPER_ADMIN.equals(user.getUsername()) && !"ADMIN".equals(role)) {
                throw new BusinessException(400, "不能修改超级管理员的角色");
            }
            user.setRole(role);
        }
        if (params.containsKey("nickname")) {
            user.setNickname((String) params.get("nickname"));
        }
        if (params.containsKey("status")) {
            int status = Integer.parseInt(params.get("status").toString());
            // 禁止禁用超级管理员
            if (SUPER_ADMIN.equals(user.getUsername()) && status == 0) {
                throw new BusinessException(400, "不能禁用超级管理员");
            }
            user.setStatus(status);
        }
        updateById(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 非admin用户无权重置超级管理员的密码
        checkSuperAdminProtection(user, "重置密码");
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 禁止删除超级管理员
        if (SUPER_ADMIN.equals(user.getUsername())) {
            throw new BusinessException(400, "不能删除超级管理员");
        }
        if ("ADMIN".equals(user.getRole())) {
            long adminCount = count(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) {
                throw new BusinessException(400, "不能删除最后一个超级管理员");
            }
        }
        removeById(id);
    }
}
