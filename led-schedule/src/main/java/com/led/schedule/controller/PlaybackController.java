package com.led.schedule.controller;

import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.common.entity.*;
import com.led.schedule.feign.DeviceFeignClient;
import com.led.schedule.mapper.ProgramItemMapper;
import com.led.schedule.service.PlayLogService;
import com.led.schedule.service.ProgramService;
import com.led.schedule.dto.PlaybackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class PlaybackController {

    private final ProgramService programService;
    private final PlayLogService playLogService;
    private final ProgramItemMapper programItemMapper;
    private final DeviceFeignClient deviceFeignClient;

    /** 开始播放节目到指定屏幕 */
    @PostMapping("/start/{programId}")
    @AuditLog(module = "播放控制", action = "PLAY", description = "开始播放节目")
    public R<Void> startPlay(@PathVariable Long programId, @RequestBody PlaybackRequest request) {
        Program program = programService.getWithItems(programId);
        if (program == null) return R.fail(404, "节目不存在");

        List<ProgramItem> items = programItemMapper.selectByProgramId(programId);
        String itemsJson = toJsonArray(items);

        for (Long screenId : request.getScreenIds()) {
            try {
                String mqttClientId = getMqttClientId(screenId);
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "play");
                cmd.put("mqttClientId", mqttClientId);
                cmd.put("programName", program.getName());
                cmd.put("items", itemsJson);
                deviceFeignClient.sendMqttCommand(cmd);
                saveLog(screenId, programId, "play", "播放节目: " + program.getName());
            } catch (Exception e) {
                saveLog(screenId, programId, "play_error", "播放失败: " + e.getMessage());
            }
        }
        return R.okMsg("播放指令已下发");
    }

    /** 停止播放 */
    @PostMapping("/stop/{programId}")
    @AuditLog(module = "播放控制", action = "STOP", description = "停止播放")
    public R<Void> stopPlay(@PathVariable Long programId, @RequestBody PlaybackRequest request) {
        for (Long screenId : request.getScreenIds()) {
            try {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("type", "stop");
                cmd.put("mqttClientId", getMqttClientId(screenId));
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

    private String getMqttClientId(Long screenId) {
        // 这里简化处理，实际应通过Feign调用led-device获取
        return "SCREEN-" + String.format("%03d", screenId);
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
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            ProgramItem item = items.get(i);
            sb.append(String.format("{\"contentId\":%d,\"sortOrder\":%d,\"duration\":%d,\"contentName\":\"%s\"}",
                    item.getContentId(), item.getSortOrder(), item.getDuration(),
                    item.getContentName() != null ? item.getContentName() : ""));
        }
        sb.append("]");
        return sb.toString();
    }
}
