package com.led.schedule.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.led.common.entity.Program;
import com.led.common.entity.ProgramItem;
import com.led.common.entity.ProgramPublish;
import com.led.schedule.mapper.ProgramPublishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramPublishService extends ServiceImpl<ProgramPublishMapper, ProgramPublish> {

    private final ProgramPublishMapper programPublishMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 获取节目的发布历史 */
    public List<ProgramPublish> getByProgramId(Long programId) {
        return programPublishMapper.selectByProgramId(programId);
    }

    /** 发布节目时创建发布记录 */
    public void createPublishRecord(Program program, String operator, String publishType,
                                     List<Map<String, Object>> targetScreens, boolean success) {
        if (program == null) return;

        ProgramPublish record = new ProgramPublish();
        record.setProgramId(program.getId());
        record.setVersion(programPublishMapper.getMaxVersion(program.getId()) + 1);
        record.setOperator(operator);
        record.setPublishType(publishType != null ? publishType : "play");
        record.setResult(success ? "success" : "fail");

        // 构建目标屏幕快照
        try {
            if (targetScreens != null && !targetScreens.isEmpty()) {
                record.setTargetScreens(objectMapper.writeValueAsString(targetScreens));
            } else {
                record.setTargetScreens("[]");
            }
        } catch (JsonProcessingException e) {
            record.setTargetScreens("[]");
        }

        // 构建节目快照
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("name", program.getName());
            snap.put("description", program.getDescription());
            snap.put("itemCount", program.getItems() != null ? program.getItems().size() : 0);
            if (program.getItems() != null) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (ProgramItem item : program.getItems()) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("contentId", item.getContentId());
                    im.put("contentName", item.getContentName());
                    im.put("contentType", item.getContentType());
                    im.put("sortOrder", item.getSortOrder());
                    im.put("duration", item.getDuration());
                    items.add(im);
                }
                snap.put("items", items);
            }
            record.setSnapshot(objectMapper.writeValueAsString(snap));
        } catch (JsonProcessingException e) {
            log.warn("[发布记录] 快照序列化失败: programId={}", program.getId(), e);
            record.setSnapshot("{}");
        }

        save(record);
        log.info("[发布记录] 节目已发布: program={}, version={}, operator={}, type={}, result={}",
                program.getName(), record.getVersion(), operator, record.getPublishType(), record.getResult());
    }
}
