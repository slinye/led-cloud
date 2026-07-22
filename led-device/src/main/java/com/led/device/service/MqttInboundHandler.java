package com.led.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.led.device.websocket.ScreenStatusWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** MQTT 入站消息监听器 — 处理屏幕上报的心跳、注册、状态消息 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttInboundHandler {

    private final ScreenService screenService;
    private final ScreenStatusWebSocket screenStatusWebSocket;
    private final ObjectMapper objectMapper;

    /** 处理屏幕上报的注册消息 */
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleInbound(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload().toString();

        log.debug("[MQTT IN] topic={}, payload={}", topic, payload);

        if (topic == null) return;

        try {
            // 从 topic 提取 mqttClientId: led/heartbeat/20000001
            String[] parts = topic.split("/");
            if (parts.length < 3) return;
            String mqttClientId = parts[2];

            if (topic.contains("/heartbeat/")) {
                screenService.updateHeartbeat(mqttClientId);
                screenStatusWebSocket.broadcastStatus(mqttClientId, "online");
            } else if (topic.contains("/register/")) {
                JsonNode json = objectMapper.readTree(payload);
                String ip = json.has("ip") ? json.get("ip").asText(null) : null;
                String model = json.has("model") ? json.get("model").asText(null) : null;
                Integer width = json.has("width") ? json.get("width").asInt(0) : null;
                Integer height = json.has("height") ? json.get("height").asInt(0) : null;
                Integer brightness = json.has("brightness") ? json.get("brightness").asInt(80) : null;
                // width/height 为 0 时按 null 处理
                if (width != null && width == 0) width = null;
                if (height != null && height == 0) height = null;
                screenService.registerOrUpdate(mqttClientId, ip, model, width, height, brightness);
                screenStatusWebSocket.broadcastStatus(mqttClientId, "online");
                log.info("[MQTT] 屏幕注册: {}", mqttClientId);
            } else if (topic.contains("/status/")) {
                // 屏幕状态变更
                screenStatusWebSocket.broadcastStatus(mqttClientId, payload);
            }
        } catch (Exception e) {
            log.error("[MQTT IN] 处理消息异常: {}", e.getMessage());
        }
    }
}
