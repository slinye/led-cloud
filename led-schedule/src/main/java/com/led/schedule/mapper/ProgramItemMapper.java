package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.ProgramItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProgramItemMapper extends BaseMapper<ProgramItem> {

    @Select("SELECT pi.*, c.name as content_name, c.type as content_type, " +
            "c.file_path, c.text_content, c.font_size, c.font_color, c.bg_color, c.scroll_speed " +
            "FROM t_program_item pi LEFT JOIN t_content c ON pi.content_id = c.id " +
            "WHERE pi.program_id = #{programId} ORDER BY pi.sort_order")
    List<ProgramItem> selectByProgramId(Long programId);

    @Delete("DELETE FROM t_program_item WHERE program_id = #{programId}")
    void deleteByProgramId(Long programId);
}
