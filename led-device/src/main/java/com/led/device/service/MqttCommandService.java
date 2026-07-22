package com.led.device.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** MQTT 命令下发服务 — 向LED屏幕发送控制指令 */
@Slf4j
@Service
public class MqttCommandService {

    private final MqttPahoMessageHandler mqttOutbound;

    @Value("${mqtt.topic-prefix}")
    private String topicPrefix;

    /** 幂等去重：key = fingerprint, value = 过期时间戳（ms） */
    private final ConcurrentHashMap<String, Long> recentCommands = new ConcurrentHashMap<>();

    public MqttCommandService(MqttPahoMessageHandler mqttOutbound) {
        this.mqttOutbound = mqttOutbound;
        // 每30秒清理过期记录
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mqtt-dedup-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            recentCommands.entrySet().removeIf(e -> e.getValue() < now);
        }, 30, 30, TimeUnit.SECONDS);
    }

    /** 播放节目 */
    public void sendPlay(String mqttClientId, String programName, String payload) {
        sendToScreen(mqttClientId, "play", "{\"type\":\"play\",\"programName\":\"" +
                escape(programName) + "\",\"items\":" + payload + "}");
    }

    /** 停止播放 */
    public void sendStop(String mqttClientId) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"stop\"}");
    }

    /** 暂停播放 */
    public void sendPause(String mqttClientId) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"pause\"}");
    }

    /** 恢复播放 */
    public void sendResume(String mqttClientId) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"resume\"}");
    }

    /** 开机 */
    public void sendPowerOn(String mqttClientId) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"power_on\"}");
    }

    /** 关机 */
    public void sendPowerOff(String mqttClientId) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"power_off\"}");
    }

    /** 调节亮度 */
    public void sendBrightness(String mqttClientId, int brightness) {
        sendToScreen(mqttClientId, "command", "{\"type\":\"brightness\",\"value\":" + brightness + "}");
    }

    /** 发送心跳 ping */
    public void sendHeartbeatPing(String mqttClientId) {
        send(mqttClientId, topicPrefix + "/ping/" + mqttClientId, "{\"type\":\"ping\"}");
    }

    private void sendToScreen(String mqttClientId, String subTopic, String payload) {
        send(mqttClientId, topicPrefix + "/command/" + mqttClientId + "/" + subTopic, payload);
    }

    private void send(String mqttClientId, String topic, String payload) {
        if (mqttClientId == null || mqttClientId.isEmpty()) {
            log.warn("[MQTT] 跳过发送，mqttClientId 为空");
            return;
        }
        // 幂等去重：同topic+同内容5秒内只发一次
        String fingerprint = topic + "|" + payload.hashCode();
        Long expired = recentCommands.get(fingerprint);
        if (expired != null && System.currentTimeMillis() < expired) {
            log.warn("[MQTT] 重复命令已拦截 -> {} (指纹: {})", topic, fingerprint);
            return;
        }
        try {
            mqttOutbound.handleMessage(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 1)
                    .build());
            recentCommands.put(fingerprint, System.currentTimeMillis() + 5000);
            log.info("[MQTT] 命令已发送 -> {} : {}", topic, payload);
        } catch (Exception e) {
            log.error("[MQTT] 发送失败 -> {} : {}", topic, e.getMessage());
            throw new RuntimeException("MQTT发送失败(" + mqttClientId + "): " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
