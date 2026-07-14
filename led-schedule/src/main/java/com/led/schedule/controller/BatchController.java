package com.led.schedule.controller;

import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.PlayLog;
import com.led.schedule.feign.DeviceFeignClient;
import com.led.schedule.service.PlayLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class BatchController {

    private final DeviceFeignClient deviceFeignClient;
    private final PlayLogService playLogService;

    /** 批量下发节目到多个屏幕 */
    @PostMapping("/deploy-program")
    @AuditLog(module = "批量操作", action = "DEPLOY", description = "批量下发节目")
    public R<Void> deployProgram(@RequestBody Map<String, Object> params) {
        Long programId = Long.valueOf(params.get("programId").toString());
        String programName = (String) params.getOrDefault("programName", "节目" + programId);
        @SuppressWarnings("unchecked")
        List<Integer> screenIds = (List<Integer>) params.get("screenIds");

        for (Integer sid : screenIds) {
            String clientId = "SCREEN-" + String.format("%03d", sid);
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "play");
                cmd.put("mqttClientId", clientId);
                cmd.put("programName", programName);
                cmd.put("items", "[]");
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(sid.longValue(), programId, "batch_start", "批量下发节目: " + programName);
            } catch (Exception ignored) {}
        }
        return R.ok();
    }

    /** 批量开关机 */
    @PostMapping("/power")
    @AuditLog(module = "批量操作", action = "POWER", description = "批量开关机")
    public R<Void> batchPower(@RequestBody Map<String, Object> params) {
        String action = (String) params.get("action");
        @SuppressWarnings("unchecked")
        List<Integer> screenIds = (List<Integer>) params.get("screenIds");

        for (Integer sid : screenIds) {
            String clientId = "SCREEN-" + String.format("%03d", sid);
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("mqttClientId", clientId);
                if ("on".equals(action)) {
                    cmd.put("type", "power_on");
                } else {
                    cmd.put("type", "power_off");
                }
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(sid.longValue(), null, "batch_power_" + action,
                        "批量" + (action.equals("on") ? "开机" : "关机"));
            } catch (Exception ignored) {}
        }
        return R.ok();
    }

    /** 批量调亮度 */
    @PostMapping("/brightness")
    @AuditLog(module = "批量操作", action = "BRIGHTNESS", description = "批量调节亮度")
    public R<Void> batchBrightness(@RequestBody Map<String, Object> params) {
        int brightness = Integer.parseInt(params.get("brightness").toString());
        @SuppressWarnings("unchecked")
        List<Integer> screenIds = (List<Integer>) params.get("screenIds");

        for (Integer sid : screenIds) {
            String clientId = "SCREEN-" + String.format("%03d", sid);
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "brightness");
                cmd.put("mqttClientId", clientId);
                cmd.put("value", brightness);
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(sid.longValue(), null, "batch_brightness", "批量调亮度: " + brightness);
            } catch (Exception ignored) {}
        }
        return R.ok();
    }

    private void saveLog(Long screenId, Long programId, String action, String message) {
        PlayLog log = new PlayLog();
        log.setScreenId(screenId);
        log.setProgramId(programId);
        log.setAction(action);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        playLogService.save(log);
    }
}
