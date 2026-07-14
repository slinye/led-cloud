package com.led.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.led.common.entity.Program;
import com.led.common.entity.ProgramItem;
import com.led.common.exception.BusinessException;
import com.led.schedule.mapper.ProgramItemMapper;
import com.led.schedule.mapper.ProgramMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramService extends ServiceImpl<ProgramMapper, Program> {

    private final ProgramItemMapper programItemMapper;

    public Page<Program> pagePrograms(int page, int size, String keyword) {
        LambdaQueryWrapper<Program> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Program::getName, keyword);
        }
        wrapper.orderByDesc(Program::getCreatedAt);
        Page<Program> result = page(new Page<>(page, size), wrapper);
        // 填充 items
        result.getRecords().forEach(p -> p.setItems(programItemMapper.selectByProgramId(p.getId())));
        return result;
    }

    public List<Program> listAll() {
        List<Program> programs = baseMapper.selectAllOrderByTime();
        programs.forEach(p -> p.setItems(programItemMapper.selectByProgramId(p.getId())));
        return programs;
    }

    public Program getWithItems(Long id) {
        Program program = getById(id);
        if (program != null) {
            program.setItems(programItemMapper.selectByProgramId(id));
        }
        return program;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createProgram(Program program, List<ProgramItem> items) {
        save(program);
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                ProgramItem item = items.get(i);
                item.setProgramId(program.getId());
                if (item.getSortOrder() == null) item.setSortOrder(i + 1);
                if (item.getDuration() == null) item.setDuration(10);
                programItemMapper.insert(item);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProgram(Program program, List<ProgramItem> items) {
        if (getById(program.getId()) == null) throw new BusinessException(404, "节目不存在");
        updateById(program);
        if (items != null) {
            programItemMapper.deleteByProgramId(program.getId());
            for (int i = 0; i < items.size(); i++) {
                ProgramItem item = items.get(i);
                item.setProgramId(program.getId());
                item.setId(null);
                if (item.getSortOrder() == null) item.setSortOrder(i + 1);
                if (item.getDuration() == null) item.setDuration(10);
                programItemMapper.insert(item);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProgram(Long id) {
        programItemMapper.deleteByProgramId(id);
        removeById(id);
    }
}
