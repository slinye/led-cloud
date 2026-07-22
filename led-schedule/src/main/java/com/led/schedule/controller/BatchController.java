package com.led.schedule.controller;

import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.PlayLog;
import com.led.common.entity.ProgramItem;
import com.led.common.entity.Screen;
import com.led.schedule.feign.DeviceFeignClient;
import com.led.schedule.mapper.ProgramItemMapper;
import com.led.schedule.mapper.ScreenMapper;
import com.led.schedule.service.PlayLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class BatchController {

    private final DeviceFeignClient deviceFeignClient;
    private final PlayLogService playLogService;
    private final ProgramItemMapper programItemMapper;
    private final ScreenMapper screenMapper;

    /** 批量下发节目到多个屏幕（支持 play / stop） */
    @PostMapping("/deploy-program")
    @AuditLog(module = "批量操作", action = "DEPLOY", description = "批量下发节目")
    public R<Void> deployProgram(@RequestBody Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "play");
        String programName = (String) params.getOrDefault("programName", "");
        @SuppressWarnings("unchecked")
        List<Integer> screenIds = (List<Integer>) params.get("screenIds");

        if ("stop".equals(action)) {
            // 停止播放：只发 stop 指令，不需要节目数据
            for (Integer sid : screenIds) {
                String clientId = getMqttClientId(sid.longValue());
                if (clientId == null) continue;
                try {
                    Map<String, Object> cmd = new HashMap<>();
                    cmd.put("type", "stop");
                    cmd.put("mqttClientId", clientId);
                    deviceFeignClient.sendMqttCommand(cmd);
                    saveLog(sid.longValue(), null, "batch_stop", "批量停止播放");
                } catch (Exception ignored) {}
            }
            return R.okMsg("批量停止指令已下发");
        }

        // 开始播放
        Long programId = Long.valueOf(params.get("programId").toString());
        if (programName.isEmpty()) {
            programName = "节目" + programId;
        }

        List<ProgramItem> items = programItemMapper.selectByProgramId(programId);
        String itemsJson = toItemJson(items);

        for (Integer sid : screenIds) {
            String clientId = getMqttClientId(sid.longValue());
            if (clientId == null) continue;
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "play");
                cmd.put("mqttClientId", clientId);
                cmd.put("programName", programName);
                cmd.put("items", itemsJson);
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(sid.longValue(), programId, "batch_start", "批量下发节目: " + programName);
            } catch (Exception ignored) {}
        }
        return R.okMsg("批量播放指令已下发");
    }


    /** 批量开关机 */
    @PostMapping("/power")
    @AuditLog(module = "批量操作", action = "POWER", description = "批量开关机")
    public R<Void> batchPower(@RequestBody Map<String, Object> params) {
        String action = (String) params.get("action");
        @SuppressWarnings("unchecked")
        List<Integer> screenIds = (List<Integer>) params.get("screenIds");

        for (Integer sid : screenIds) {
            String clientId = getMqttClientId(sid.longValue());
            if (clientId == null) continue;
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
            String clientId = getMqttClientId(sid.longValue());
            if (clientId == null) continue;
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

    /** 从数据库获取设备的真实 mqttClientId（2开头的8位数字） */
    private String getMqttClientId(Long screenId) {
        Screen screen = screenMapper.selectById(screenId);
        if (screen == null || screen.getMqttClientId() == null || screen.getMqttClientId().isEmpty()) {
            log.warn("[批量操作] 屏幕 {} 未注册或无 mqttClientId，跳过", screenId);
            return null;
        }
        return screen.getMqttClientId();
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

    private String toItemJson(List<ProgramItem> items) {
        if (items == null || items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            ProgramItem item = items.get(i);
            sb.append(String.format("{\"contentId\":%d,\"sortOrder\":%d,\"duration\":%d,\"contentName\":\"%s\",\"contentType\":\"%s\",\"filePath\":\"%s\",\"textContent\":\"%s\"}",
                    item.getContentId(),
                    item.getSortOrder() != null ? item.getSortOrder() : 0,
                    item.getDuration() != null ? item.getDuration() : 10,
                    item.getContentName() != null ? item.getContentName() : "",
                    item.getContentType() != null ? item.getContentType() : "",
                    item.getFilePath() != null ? item.getFilePath() : "",
                    item.getTextContent() != null ? item.getTextContent() : ""));
        }
        sb.append("]");
        return sb.toString();
    }
}

