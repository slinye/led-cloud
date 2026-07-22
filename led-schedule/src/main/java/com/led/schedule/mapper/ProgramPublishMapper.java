package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.ProgramPublish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProgramPublishMapper extends BaseMapper<ProgramPublish> {
    @Select("SELECT * FROM t_program_publish WHERE program_id = #{programId} ORDER BY created_at DESC")
    List<ProgramPublish> selectByProgramId(Long programId);

    @Select("SELECT COALESCE(MAX(version), 0) FROM t_program_publish WHERE program_id = #{programId}")
    int getMaxVersion(Long programId);
}
