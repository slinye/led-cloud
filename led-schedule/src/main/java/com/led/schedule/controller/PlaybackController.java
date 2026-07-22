package com.led.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.*;
import com.led.schedule.feign.DeviceFeignClient;
import com.led.schedule.mapper.ProgramItemMapper;
import com.led.schedule.mapper.ScreenMapper;
import com.led.schedule.service.PlayLogService;
import com.led.schedule.service.ProgramPublishService;
import com.led.schedule.service.ProgramService;
import com.led.schedule.dto.PlaybackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class PlaybackController {

    private final ProgramService programService;
    private final PlayLogService playLogService;
    private final ProgramPublishService programPublishService;
    private final ProgramItemMapper programItemMapper;
    private final DeviceFeignClient deviceFeignClient;
    private final ScreenMapper screenMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /** 开始播放节目到指定屏幕 */
    @PostMapping("/start/{programId}")
    @AuditLog(module = "播放控制", action = "PLAY", description = "开始播放节目")
    public R<Void> startPlay(@PathVariable Long programId, @RequestBody PlaybackRequest request) {
        Program program = programService.getWithItems(programId);
        if (program == null) return R.fail(404, "节目不存在");

        List<ProgramItem> items = programItemMapper.selectByProgramId(programId);
        String itemsJson = toJsonArray(items);

        boolean hasError = false;
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Long screenId : request.getScreenIds()) {
            Map<String, Object> target = new HashMap<>();
            target.put("id", screenId);
            target.put("name", getMqttClientId(screenId));
            targets.add(target);
            try {
                String mqttClientId = getMqttClientId(screenId);
                if (mqttClientId == null) continue;
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "play");
                cmd.put("mqttClientId", mqttClientId);
                cmd.put("programName", program.getName());
                cmd.put("items", itemsJson);
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(screenId, programId, "play", "播放节目: " + program.getName());
            } catch (Exception e) {
                hasError = true;
                saveLog(screenId, programId, "play_error", "播放失败: " + e.getMessage());
            }
        }
        // 记录发布历史
        try {
            programPublishService.createPublishRecord(program, getCurrentUsername(), "play", targets, !hasError);
        } catch (Exception e) {
            // 发布历史记录失败不影响播放
        }
        return R.okMsg("播放指令已下发");
    }

    /** 停止播放 */
    @PostMapping("/stop/{programId}")
    @AuditLog(module = "播放控制", action = "STOP", description = "停止播放")
    public R<Void> stopPlay(@PathVariable Long programId, @RequestBody PlaybackRequest request) {
        for (Long screenId : request.getScreenIds()) {
            try {
                String mqttClientId = getMqttClientId(screenId);
                if (mqttClientId == null) continue;
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "stop");
                cmd.put("mqttClientId", mqttClientId);
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(screenId, programId, "stop", "停止播放");
            } catch (Exception ignored) {}
        }
        return R.okMsg("停止指令已下发");
    }

    /** 屏幕控制: pause/resume/stop/brightness */
    @PostMapping("/control/{screenId}")
    @AuditLog(module = "播放控制", action = "CONTROL", description = "屏幕控制")
    public R<Void> control(@PathVariable Long screenId, @RequestBody Map<String, Object> params) {
        String action = (String) params.get("action");
        String mqttClientId = getMqttClientId(screenId);
        if (mqttClientId == null) {
            return R.fail(400, "设备未注册，无 MQTT ClientId");
        }

        Map<String, Object> cmd = new HashMap<>();
        cmd.put("mqttClientId", mqttClientId);

        switch (action) {
            case "pause":
                cmd.put("type", "pause");
                break;
            case "resume":
                cmd.put("type", "resume");
                break;
            case "stop":
                cmd.put("type", "stop");
                break;
            case "brightness":
                cmd.put("type", "brightness");
                cmd.put("value", params.get("brightness"));
                break;
            default:
                return R.fail(400, "未知操作: " + action);
        }

        deviceFeignClient.sendMqttCommand(cmd);
        saveLog(screenId, null, "control_" + action, "屏幕控制: " + action);
        return R.okMsg("控制指令已下发");
    }

    /** 从数据库获取设备的真实 mqttClientId（2开头的8位数字） */
    private String getMqttClientId(Long screenId) {
        Screen screen = screenMapper.selectById(screenId);
        if (screen == null || screen.getMqttClientId() == null || screen.getMqttClientId().isEmpty()) {
            log.warn("[播放控制] 屏幕 {} 未注册或无 mqttClientId，跳过下发", screenId);
            return null;
        }
        return screen.getMqttClientId();
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal != null ? principal.toString() : "system";
        } catch (Exception e) {
            return "system";
        }
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

    private String toJsonArray(List<ProgramItem> items) {
        if (items == null || items.isEmpty()) return "[]";
        List<Map<String, Object>> list = new ArrayList<>(items.size());
        for (ProgramItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("contentId", item.getContentId());
            map.put("sortOrder", item.getSortOrder());
            map.put("duration", item.getDuration());
            map.put("contentName", item.getContentName());
            map.put("contentType", item.getContentType());
            map.put("filePath", item.getFilePath());
            map.put("textContent", item.getTextContent());
            map.put("fontSize", item.getFontSize());
            map.put("fontColor", item.getFontColor());
            map.put("bgColor", item.getBgColor());
            map.put("scrollSpeed", item.getScrollSpeed());
            list.add(map);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("序列化节目项失败", e);
        }
    }

}
