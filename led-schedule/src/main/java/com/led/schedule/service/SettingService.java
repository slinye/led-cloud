package com.led.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.Setting;
import com.led.schedule.mapper.SettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingService extends ServiceImpl<SettingMapper, Setting> {

    public Map<String, String> getAllAsMap() {
        List<Setting> settings = list();
        Map<String, String> map = new HashMap<>();
        for (Setting s : settings) {
            map.put(s.getSettingKey(), s.getSettingValue() != null ? s.getSettingValue() : "");
        }
        return map;
    }

    @Transactional
    public void updateFromMap(Map<String, String> body) {
        for (Map.Entry<String, String> entry : body.entrySet()) {
            Setting setting = getOne(new LambdaQueryWrapper<Setting>()
                    .eq(Setting::getSettingKey, entry.getKey()));
            if (setting != null) {
                setting.setSettingValue(entry.getValue());
                updateById(setting);
            }
        }
    }
}
