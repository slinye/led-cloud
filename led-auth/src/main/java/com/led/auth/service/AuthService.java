package com.led.auth.service;

import com.led.auth.config.WechatConfig;
import com.led.auth.mapper.UserMapper;
import com.led.common.dto.ChangePasswordRequest;
import com.led.common.dto.LoginRequest;
import com.led.common.dto.WechatLoginRequest;
import com.led.common.entity.User;
import com.led.common.exception.BusinessException;
import com.led.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final WechatConfig wechatConfig;

    private static final String WECHAT_CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /** 登录，返回 JWT Token */
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
        return buildLoginResult(user);
    }

    /**
     * 微信小程序登录 - wx.login() code 换 openid 后签发 JWT
     */
    @Transactional
    public Map<String, Object> loginByWechat(WechatLoginRequest request) {
        // 1. 向微信服务器换取 openid
        String openid = code2openid(request.getCode());
        if (openid == null) {
            throw new BusinessException(401, "微信登录失败，请重试");
        }

        // 2. 查找是否已有绑定用户
        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            // 创建新用户（默认 VIEWER 角色）
            user = createWechatUser(openid, request.getNickName());
        }

        // 3. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        return buildLoginResult(user);
    }

    /**
     * 调用微信 code2session 接口换取 openid
     */
    private String code2openid(String code) {
        try {
            String url = String.format(WECHAT_CODE2SESSION_URL,
                    wechatConfig.getAppid(), wechatConfig.getSecret(), code);
            log.info("请求微信 code2session...");
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) {
                log.error("微信 code2session 返回为空");
                return null;
            }
            Object errcode = resp.get("errcode");
            if (errcode != null && (int) errcode != 0) {
                log.error("微信 code2session 失败: errcode={}, errmsg={}", errcode, resp.get("errmsg"));
                return null;
            }
            String openid = (String) resp.get("openid");
            log.info("获取 openid 成功: {}", openid);
            return openid;
        } catch (Exception e) {
            log.error("调用微信 code2session 异常", e);
            return null;
        }
    }

    /**
     * 创建微信用户（默认 VIEWER 角色）
     */
    private User createWechatUser(String openid, String nickname) {
        User user = new User();
        user.setUsername("wx_" + openid.substring(0, Math.min(openid.length(), 16)));
        user.setPassword(passwordEncoder.encode(openid)); // 用 openid hash 作为密码占位
        user.setNickname(nickname != null ? nickname : "微信用户");
        user.setRole("VIEWER");
        user.setStatus(1);
        user.setWechatOpenid(openid);
        userMapper.insert(user);
        log.info("创建微信用户成功: id={}, openid={}", user.getId(), openid);
        return user;
    }

    /** 构造登录返回结果 */
    private Map<String, Object> buildLoginResult(User user) {
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

    /** 验证 Token 有效性 */
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

    /** 修改密码 */
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

    /** 获取角色列表 */
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
