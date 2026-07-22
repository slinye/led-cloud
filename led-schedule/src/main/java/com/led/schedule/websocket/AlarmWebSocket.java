package com.led.schedule.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 告警 WebSocket — 实时推送告警通知给前端 */
@Slf4j
@Component
@ServerEndpoint("/ws/alarm")
public class AlarmWebSocket {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.info("[告警WS] 客户端连接: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.info("[告警WS] 客户端断开: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("[告警WS] 错误: {}", error.getMessage());
        sessions.remove(session.getId());
    }

    /** 广播告警消息给所有连接的客户端 */
    public void broadcast(String jsonMessage) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(jsonMessage);
                }
            } catch (IOException e) {
                log.warn("[告警WS] 发送失败: {}", e.getMessage());
            }
        });
    }

    /** 获取当前连接数 */
    public int getOnlineCount() {
        return sessions.size();
    }
}
