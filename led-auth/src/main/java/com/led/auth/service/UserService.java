package com.led.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.auth.mapper.UserMapper;
import com.led.common.entity.User;
import com.led.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

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
        // 检查用户名唯一
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        // 校验角色
        if (!java.util.Arrays.asList("ADMIN", "OPERATOR", "VIEWER").contains(user.getRole())) {
            throw new BusinessException(400, "无效的角色");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 不能删除最后一个 admin
        if ("ADMIN".equals(user.getRole())) {
            long adminCount = count(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) {
                throw new BusinessException(400, "不能删除最后一个超级管理员");
            }
        }
        removeById(id);
    }
}
