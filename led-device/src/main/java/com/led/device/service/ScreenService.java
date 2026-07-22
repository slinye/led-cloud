package com.led.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.Screen;
import com.led.common.exception.BusinessException;
import com.led.device.mapper.ScreenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenService extends ServiceImpl<ScreenMapper, Screen> {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^2\\d{7}$");

    private void validateMqttClientId(String mqttClientId) {
        if (mqttClientId == null || mqttClientId.trim().isEmpty()) {
            throw new BusinessException(400, "设备ID不能为空");
        }
        if (!DEVICE_ID_PATTERN.matcher(mqttClientId).matches()) {
            throw new BusinessException(400, "设备ID必须是2开头的8位阿拉伯数字");
        }
    }

    public Page<Screen> pageScreens(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Screen::getName, keyword).or()
                    .like(Screen::getMqttClientId, keyword).or()
                    .like(Screen::getIpAddress, keyword));
        }
        // 基于心跳时间的在线/离线判断（60秒内有心跳=在线）
        if (status != null && !status.isEmpty()) {
            if ("online".equals(status)) {
                wrapper.apply("last_heartbeat >= DATE_SUB(NOW(), INTERVAL 60 SECOND)");
            } else if ("offline".equals(status)) {
                wrapper.apply("last_heartbeat < DATE_SUB(NOW(), INTERVAL 60 SECOND) OR last_heartbeat IS NULL");
            }
        }
        wrapper.orderByDesc(Screen::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void createScreen(Screen screen) {
        validateMqttClientId(screen.getMqttClientId());
        // 检查 mqttClientId 唯一性
        LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Screen::getMqttClientId, screen.getMqttClientId());
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "设备ID已存在");
        }
        if (screen.getStatus() == null) screen.setStatus("offline");
        if (screen.getBrightness() == null) screen.setBrightness(80);
        save(screen);
    }

    @Transactional
    public void updateScreen(Screen screen) {
        Screen existing = getById(screen.getId());
        if (existing == null) throw new BusinessException(404, "屏幕不存在");
        if (screen.getMqttClientId() != null) {
            validateMqttClientId(screen.getMqttClientId());
            if (!screen.getMqttClientId().equals(existing.getMqttClientId())) {
                LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Screen::getMqttClientId, screen.getMqttClientId());
                if (count(wrapper) > 0) {
                    throw new BusinessException(400, "设备ID已存在");
                }
            }
        }
        updateById(screen);
    }

    @Transactional
    public void updateHeartbeat(String mqttClientId) {
        Screen screen = baseMapper.selectByMqttClientId(mqttClientId);
        if (screen != null) {
            screen.setLastHeartbeat(LocalDateTime.now());
            screen.setStatus("online");
            updateById(screen);
        }
    }

    @Transactional
    public void updateStatus(String mqttClientId, String status) {
        Screen screen = baseMapper.selectByMqttClientId(mqttClientId);
        if (screen != null) {
            screen.setStatus(status);
            updateById(screen);
        }
    }

    /**
     * 设备注册：已存在则更新状态和心跳，不存在则自动创建。
     * 自动生成的设备名称为 "自动注册-<mqttClientId>"。
     */
    @Transactional
    public Screen registerOrUpdate(String mqttClientId, String ip, String model,
                                   Integer width, Integer height, Integer brightness) {
        validateMqttClientId(mqttClientId);
        Screen screen = baseMapper.selectByMqttClientId(mqttClientId);
        LocalDateTime now = LocalDateTime.now();
        if (screen == null) {
            screen = new Screen();
            screen.setMqttClientId(mqttClientId);
            screen.setName("自动注册-" + mqttClientId);
            screen.setIpAddress(ip);
            screen.setModel(model);
            screen.setResolutionWidth(width);
            screen.setResolutionHeight(height);
            screen.setBrightness(brightness != null ? brightness : 80);
            screen.setStatus("online");
            screen.setLastHeartbeat(now);
            screen.setLocation("未设置");
            save(screen);
            log.info("[Screen] 自动注册新设备: {}", mqttClientId);
        } else {
            screen.setStatus("online");
            screen.setLastHeartbeat(now);
            if (ip != null) screen.setIpAddress(ip);
            if (model != null) screen.setModel(model);
            if (width != null) screen.setResolutionWidth(width);
            if (height != null) screen.setResolutionHeight(height);
            if (brightness != null) screen.setBrightness(brightness);
            updateById(screen);
            log.info("[Screen] 设备已注册，更新状态: {}", mqttClientId);
        }
        return screen;
    }

    public Screen getByMqttClientId(String mqttClientId) {
        return baseMapper.selectByMqttClientId(mqttClientId);
    }

    @Transactional
    public void deleteScreen(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "屏幕不存在");
        removeById(id);
    }
}
