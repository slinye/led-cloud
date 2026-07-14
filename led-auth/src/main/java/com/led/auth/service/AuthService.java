package com.led.auth.service;

import com.led.auth.mapper.UserMapper;
import com.led.common.dto.ChangePasswordRequest;
import com.led.common.dto.LoginRequest;
import com.led.common.entity.User;
import com.led.common.exception.BusinessException;
import com.led.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录，返回 JWT Token
     */
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        claims.put("role", user.getRole());

        String token = JwtUtil.generateToken(claims);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        return result;
    }

    /**
     * 验证 Token 有效性
     */
    public Map<String, Object> verify(String token) {
        if (!JwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Token无效或已过期");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("username", JwtUtil.getUsername(token));
        result.put("userId", JwtUtil.getUserId(token));
        result.put("role", JwtUtil.getRole(token));
        result.put("valid", true);
        return result;
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 获取角色列表
     */
    public List<Map<String, String>> getRoles() {
        Map<String, String> admin = new HashMap<>();
        admin.put("value", "ADMIN"); admin.put("label", "超级管理员");
        Map<String, String> operator = new HashMap<>();
        operator.put("value", "OPERATOR"); operator.put("label", "操作员");
        Map<String, String> viewer = new HashMap<>();
        viewer.put("value", "VIEWER"); viewer.put("label", "观察者");
        return java.util.Arrays.asList(admin, operator, viewer);
    }
}
