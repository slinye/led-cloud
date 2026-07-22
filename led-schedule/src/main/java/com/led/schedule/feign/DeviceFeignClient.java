package com.led.schedule.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** 通过Feign调用设备管理服务的MQTT命令下发 */
@FeignClient(name = "led-device", path = "/api/internal", configuration = com.led.schedule.config.FeignConfig.class)
public interface DeviceFeignClient {

    /** 内部API: 发送MQTT命令（供调度服务调用） */
    @PostMapping("/mqtt/send")
    Map<String, Object> sendMqttCommand(@RequestBody Map<String, Object> command);

    /** 内部API: 批量发送MQTT命令 */
    @PostMapping("/mqtt/batch-send")
    Map<String, Object> batchSendMqttCommand(@RequestBody Map<String, Object> batchCommand);
}
