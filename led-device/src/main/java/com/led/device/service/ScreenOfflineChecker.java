package com.led.device.service;

import com.led.device.mapper.ScreenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 定时向在线屏幕发送心跳ping并检测离线 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScreenOfflineChecker {

    private final ScreenMapper screenMapper;
    private final MqttCommandService mqttCommandService;

    /** 每30秒检查一次在线屏幕并发送心跳 */
    @Scheduled(fixedRate = 30000)
    public void checkAndPing() {
        List<com.led.common.entity.Screen> onlineScreens = screenMapper.selectOnlineScreens();
        if (onlineScreens.isEmpty()) return;

        log.info("[心跳推送] 已向 {} 台在线大屏发送心跳ping", onlineScreens.size());
        for (com.led.common.entity.Screen screen : onlineScreens) {
            try {
                mqttCommandService.sendHeartbeatPing(screen.getMqttClientId());
            } catch (Exception e) {
                log.warn("[心跳推送] 屏幕{} ({}): {}", screen.getId(), screen.getMqttClientId(), e.getMessage());
            }
        }
    }
}
