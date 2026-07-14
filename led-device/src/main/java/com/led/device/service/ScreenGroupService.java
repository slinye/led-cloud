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
}
