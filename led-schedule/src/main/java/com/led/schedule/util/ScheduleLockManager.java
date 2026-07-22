package com.led.schedule.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时调度按 scheduleId 互斥执行，防止手动触发和扫描任务并发导致重复发送 MQTT。
 */
@Component
public class ScheduleLockManager {

    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    public Object getLock(Long scheduleId) {
        return locks.computeIfAbsent(scheduleId, k -> new Object());
    }
}
