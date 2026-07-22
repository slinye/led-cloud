package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    /** 内容使用统计：每个内容被多少个节目引用 */
    @Select("SELECT c.id, c.name, c.type, COUNT(pi.id) as referenceCount " +
            "FROM t_content c LEFT JOIN t_program_item pi ON c.id = pi.content_id " +
            "GROUP BY c.id, c.name, c.type ORDER BY referenceCount DESC")
    List<Map<String, Object>> usageStats();

    /** 内容类型分布：每种类型有多少个内容 */
    @Select("SELECT type AS name, COUNT(*) AS value FROM t_content GROUP BY type ORDER BY value DESC")
    List<Map<String, Object>> typeDistribution();

    /** 查询引用某个内容的所有节目详情 */
    @Select("SELECT p.id, p.name, pi.sort_order " +
            "FROM t_program_item pi JOIN t_program p ON pi.program_id = p.id " +
            "WHERE pi.content_id = #{contentId}")
    List<Map<String, Object>> getReferencingPrograms(@Param("contentId") Long contentId);
}
