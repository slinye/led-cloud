package com.led.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.ScreenGroupRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScreenGroupRelMapper extends BaseMapper<ScreenGroupRel> {
    @Select("SELECT * FROM t_screen_group_rel WHERE group_id = #{groupId}")
    List<ScreenGroupRel> selectByGroupId(Long groupId);

    @Delete("DELETE FROM t_screen_group_rel WHERE group_id = #{groupId}")
    void deleteByGroupId(Long groupId);
}
