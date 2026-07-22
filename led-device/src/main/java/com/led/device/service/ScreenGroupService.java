package com.led.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.ScreenGroup;
import com.led.common.entity.ScreenGroupRel;
import com.led.device.mapper.ScreenGroupMapper;
import com.led.device.mapper.ScreenGroupRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenGroupService extends ServiceImpl<ScreenGroupMapper, ScreenGroup> {

    private final ScreenGroupRelMapper relMapper;

    public List<ScreenGroup> listAll() {
        return list(new LambdaQueryWrapper<ScreenGroup>().orderByDesc(ScreenGroup::getCreatedAt));
    }

    @Transactional
    public void createGroup(ScreenGroup group) {
        save(group);
    }

    @Transactional
    public void updateGroup(ScreenGroup group) {
        updateById(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        relMapper.deleteByGroupId(id);
        removeById(id);
    }

    @Transactional
    public void addScreens(Long groupId, List<Long> screenIds) {
        if (screenIds == null || screenIds.isEmpty()) {
            // 清空该分组的所有关联
            relMapper.deleteByGroupId(groupId);
            return;
        }
        // 1. 先从其他分组中移除这些屏幕（确保一屏只属于一个分组）
        relMapper.deleteByScreenIds(screenIds);
        // 2. 清空当前分组的所有旧关联（实现"替换"语义，匹配前端穿梭框行为）
        relMapper.deleteByGroupId(groupId);
        // 3. 插入新关联
        for (Long screenId : screenIds) {
            ScreenGroupRel rel = new ScreenGroupRel();
            rel.setGroupId(groupId);
            rel.setScreenId(screenId);
            relMapper.insert(rel);
        }
    }

    public List<ScreenGroupRel> getGroupScreens(Long groupId) {
        return relMapper.selectByGroupId(groupId);
    }

    /** 获取所有已被分组的屏幕ID（排除指定分组，用于过滤可选列表） */
    public List<Long> getAssignedScreenIds(Long excludeGroupId) {
        return relMapper.selectAssignedScreenIds(excludeGroupId);
    }
}
