package com.led.device.service;

import com.led.common.entity.Screen;
import com.led.device.mapper.ScreenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 定时检测在线屏幕并发送心跳ping，同时标记超时设备为离线 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScreenOfflineChecker {

    private final ScreenMapper screenMapper;
    private final ScreenService screenService;
    private final MqttCommandService mqttCommandService;

    /** 每30秒：发送心跳ping + 标记超时设备为离线 */
    @Scheduled(fixedRate = 30000)
    public void checkAndPing() {
        // 1. 先将心跳超时的设备标记为离线
        List<Screen> timedOut = screenMapper.selectTimedOutScreens();
        if (!timedOut.isEmpty()) {
            for (Screen s : timedOut) {
                s.setStatus("offline");
                screenService.updateById(s);
                log.info("[离线检测] 设备心跳超时，标记为离线: {} ({})", s.getMqttClientId(), s.getName());
            }
        }

        // 2. 只向真正在线的设备发送心跳ping
        List<Screen> onlineScreens = screenMapper.selectOnlineScreens();
        if (onlineScreens.isEmpty()) return;

        log.info("[心跳推送] 已向 {} 台在线大屏发送心跳ping", onlineScreens.size());
        for (Screen screen : onlineScreens) {
            try {
                mqttCommandService.sendHeartbeatPing(screen.getMqttClientId());
            } catch (Exception e) {
                log.warn("[心跳推送] 屏幕{} ({}): {}", screen.getId(), screen.getMqttClientId(), e.getMessage());
            }
        }
    }
}
