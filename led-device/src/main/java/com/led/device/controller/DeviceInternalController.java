package com.led.device.controller;

import com.led.device.service.MqttCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 内部 API — 供其他微服务通过 Feign 调用设备MQTT下发能力 */
@Slf4j
@RestController
@RequestMapping("/api/internal/mqtt")
@RequiredArgsConstructor
public class DeviceInternalController {

    private final MqttCommandService mqttCommandService;

    /** 发送单条MQTT命令 */
    @PostMapping("/send")
    public Map<String, Object> sendMqttCommand(@RequestBody Map<String, Object> command) {
        String type = (String) command.get("type");
        String mqttClientId = (String) command.get("mqttClientId");

        Map<String, Object> result = new HashMap<>();
        try {
            switch (type) {
                case "power_on":
                    mqttCommandService.sendPowerOn(mqttClientId);
                    break;
                case "power_off":
                    mqttCommandService.sendPowerOff(mqttClientId);
                    break;
                case "stop":
                    mqttCommandService.sendStop(mqttClientId);
                    break;
                case "pause":
                    mqttCommandService.sendPause(mqttClientId);
                    break;
                case "resume":
                    mqttCommandService.sendResume(mqttClientId);
                    break;
                case "brightness":
                    int brightness = Integer.parseInt(command.get("value").toString());
                    mqttCommandService.sendBrightness(mqttClientId, brightness);
                    break;
                case "play":
                    String programName = (String) command.get("programName");
                    String items = (String) command.getOrDefault("items", "[]").toString();
                    mqttCommandService.sendPlay(mqttClientId, programName, items);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "未知命令类型: " + type);
                    return result;
            }
            result.put("success", true);
            result.put("message", "命令已发送");
        } catch (Exception e) {
            log.error("[内部API] MQTT发送失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 批量发送 */
    @PostMapping("/batch-send")
    public Map<String, Object> batchSendMqttCommand(@RequestBody Map<String, Object> batchCommand) {
        String type = (String) batchCommand.get("type");
        @SuppressWarnings("unchecked")
        List<String> clientIds = (List<String>) batchCommand.get("mqttClientIds");

        int success = 0, fail = 0;
        for (String clientId : clientIds) {
            try {
                Map<String, Object> cmd = new HashMap<>(batchCommand);
                cmd.put("mqttClientId", clientId);
                sendMqttCommand(cmd);
                success++;
            } catch (Exception e) {
                log.warn("[内部API] 批量发送失败 {}: {}", clientId, e.getMessage());
                fail++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", clientIds.size());
        result.put("succeeded", success);
        result.put("failed", fail);
        return result;
    }
}
