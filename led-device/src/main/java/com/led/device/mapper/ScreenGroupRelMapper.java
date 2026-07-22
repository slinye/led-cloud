package com.led.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.ScreenGroupRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScreenGroupRelMapper extends BaseMapper<ScreenGroupRel> {
    @Select("SELECT * FROM t_screen_group_rel WHERE group_id = #{groupId}")
    List<ScreenGroupRel> selectByGroupId(Long groupId);

    @Delete("DELETE FROM t_screen_group_rel WHERE group_id = #{groupId}")
    void deleteByGroupId(Long groupId);

    /** 从其他分组中移除这些屏幕（确保屏幕互斥，一个屏幕只能属于一个分组） */
    @Delete("<script>DELETE FROM t_screen_group_rel WHERE screen_id IN "
            + "<foreach collection='screenIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByScreenIds(@Param("screenIds") List<Long> screenIds);

    /** 查询所有已分配（属于任意分组）的屏幕ID，排除指定分组 */
    @Select("<script>SELECT DISTINCT screen_id FROM t_screen_group_rel"
            + "<if test='excludeGroupId != null'> WHERE group_id &lt;&gt; #{excludeGroupId}</if>"
            + "</script>")
    List<Long> selectAssignedScreenIds(@Param("excludeGroupId") Long excludeGroupId);
}
