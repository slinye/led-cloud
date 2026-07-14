package com.led.device.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 屏幕状态 WebSocket — 向前端实时推送屏幕上下线状态 */
@Slf4j
@Component
@ServerEndpoint("/ws/screen-status")
public class ScreenStatusWebSocket {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.info("[WebSocket] 客户端连接: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.info("[WebSocket] 客户端断开: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("[WebSocket] 错误: {}", error.getMessage());
        sessions.remove(session.getId());
    }

    /** 广播屏幕状态给所有连接的客户端 */
    public void broadcastStatus(String mqttClientId, String status) {
        String message = String.format("{\"mqttClientId\":\"%s\",\"status\":\"%s\"}", mqttClientId, status);
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.warn("[WebSocket] 发送失败: {}", e.getMessage());
            }
        });
    }
}
