package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.entity.PlayLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PlayLogMapper extends BaseMapper<PlayLog> {

    @Select("SELECT * FROM t_play_log WHERE screen_id = #{screenId} ORDER BY created_at DESC")
    List<PlayLog> selectByScreenId(@Param("screenId") Long screenId);

    /** 分页查询播放日志（关联屏幕和节目名称，支持多条件筛选+时间范围） */
    @Select("<script>" +
        "SELECT t.id, t.screen_id, t.program_id, t.action, t.message, t.created_at, " +
        "s.name AS screen_name, p.name AS program_name, " +
        "CASE WHEN t.action LIKE '%error%' OR t.action LIKE '%fail%' THEN 'fail' ELSE 'success' END AS status " +
        "FROM t_play_log t " +
        "LEFT JOIN t_screen s ON t.screen_id = s.id " +
        "LEFT JOIN t_program p ON t.program_id = p.id " +
        "<where>" +
        "<if test='screenName != null and screenName != \"\"'>AND s.name LIKE CONCAT('%', #{screenName}, '%')</if>" +
        "<if test='programName != null and programName != \"\"'>AND p.name LIKE CONCAT('%', #{programName}, '%')</if>" +
        "<if test='status != null and status != \"\"'>" +
            "<choose>" +
            "<when test='status == \"success\"'>AND (t.action NOT LIKE '%error%' AND t.action NOT LIKE '%fail%')</when>" +
            "<otherwise>AND (t.action LIKE '%error%' OR t.action LIKE '%fail%')</otherwise>" +
            "</choose>" +
        "</if>" +
        "<if test='startTime != null'>AND t.created_at &gt;= #{startTime}</if>" +
        "<if test='endTime != null'>AND t.created_at &lt;= #{endTime}</if>" +
        "</where>" +
        "ORDER BY t.created_at DESC" +
        "</script>")
    IPage<PlayLog> selectPageWithDetails(Page<PlayLog> page,
                                          @Param("screenName") String screenName,
                                          @Param("programName") String programName,
                                          @Param("status") String status,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /** 今日每小时播放量统计（返回 0-23 小时的数据，补零） */
    @Select("SELECT HOUR(created_at) AS hour, COUNT(*) AS count " +
            "FROM t_play_log WHERE created_at >= CURDATE() " +
            "GROUP BY HOUR(created_at) ORDER BY hour")
    List<Map<String, Object>> countTodayByHour();

    /** 不分页查询导出用数据 */
    @Select("<script>" +
        "SELECT t.id, t.screen_id, t.program_id, t.action, t.message, t.created_at, " +
        "s.name AS screen_name, p.name AS program_name, " +
        "CASE WHEN t.action LIKE '%error%' OR t.action LIKE '%fail%' THEN 'fail' ELSE 'success' END AS status " +
        "FROM t_play_log t " +
        "LEFT JOIN t_screen s ON t.screen_id = s.id " +
        "LEFT JOIN t_program p ON t.program_id = p.id " +
        "<where>" +
        "<if test='screenName != null and screenName != \"\"'>AND s.name LIKE CONCAT('%', #{screenName}, '%')</if>" +
        "<if test='programName != null and programName != \"\"'>AND p.name LIKE CONCAT('%', #{programName}, '%')</if>" +
        "<if test='status != null and status != \"\"'>" +
            "<choose>" +
            "<when test='status == \"success\"'>AND (t.action NOT LIKE '%error%' AND t.action NOT LIKE '%fail%')</when>" +
            "<otherwise>AND (t.action LIKE '%error%' OR t.action LIKE '%fail%')</otherwise>" +
            "</choose>" +
        "</if>" +
        "<if test='startTime != null'>AND t.created_at &gt;= #{startTime}</if>" +
        "<if test='endTime != null'>AND t.created_at &lt;= #{endTime}</if>" +
        "</where>" +
        "ORDER BY t.created_at DESC LIMIT 50000" +
        "</script>")
    List<PlayLog> selectAllForExport(@Param("screenName") String screenName,
                                      @Param("programName") String programName,
                                      @Param("status") String status,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
}
