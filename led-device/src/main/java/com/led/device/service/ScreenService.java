package com.led.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.Screen;
import com.led.common.exception.BusinessException;
import com.led.device.mapper.ScreenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScreenService extends ServiceImpl<ScreenMapper, Screen> {

    public Page<Screen> pageScreens(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Screen::getName, keyword).or()
                    .like(Screen::getMqttClientId, keyword).or()
                    .like(Screen::getIpAddress, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Screen::getStatus, status);
        }
        wrapper.orderByDesc(Screen::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void createScreen(Screen screen) {
        // 检查 mqttClientId 唯一性
        if (screen.getMqttClientId() != null) {
            LambdaQueryWrapper<Screen> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Screen::getMqttClientId, screen.getMqttClientId());
            if (count(wrapper) > 0) {
                throw new BusinessException(400, "MQTT Client ID 已存在");
            }
        }
        if (screen.getStatus() == null) screen.setStatus("offline");
        if (screen.getBrightness() == null) screen.setBrightness(80);
        save(screen);
    }

    @Transactional
    public void updateScreen(Screen screen) {
        Screen existing = getById(screen.getId());
        if (existing == null) throw new BusinessException(404, "屏幕不存在");
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

    public Screen getByMqttClientId(String mqttClientId) {
        return baseMapper.selectByMqttClientId(mqttClientId);
    }

    @Transactional
    public void deleteScreen(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "屏幕不存在");
        removeById(id);
    }
}
