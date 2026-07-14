package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Program;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProgramMapper extends BaseMapper<Program> {
    @Select("SELECT * FROM t_program ORDER BY created_at DESC")
    List<Program> selectAllOrderByTime();
}
