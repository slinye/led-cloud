package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Screen;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScreenMapper extends BaseMapper<Screen> {

    /** 基于最近心跳时间判断在线（默认60秒内有心跳 = 在线） */
    @Select("SELECT COUNT(*) FROM t_screen WHERE last_heartbeat >= DATE_SUB(NOW(), INTERVAL #{timeoutSeconds} SECOND)")
    long countOnline(@Param("timeoutSeconds") int timeoutSeconds);

    /** 统计正在播放的屏幕数 */
    @Select("SELECT COUNT(*) FROM t_screen WHERE status = 'playing'")
    long countPlaying();

    /** 按分组统计在线/离线/播放中 */
    @Select("SELECT sg.id as groupId, sg.name as groupName, " +
            "COUNT(s.id) as total, " +
            "SUM(CASE WHEN s.last_heartbeat >= DATE_SUB(NOW(), INTERVAL #{timeoutSeconds} SECOND) THEN 1 ELSE 0 END) as online, " +
            "SUM(CASE WHEN s.status = 'playing' THEN 1 ELSE 0 END) as playing " +
            "FROM t_screen_group sg " +
            "LEFT JOIN t_screen_group_rel sgr ON sg.id = sgr.group_id " +
            "LEFT JOIN t_screen s ON sgr.screen_id = s.id " +
            "GROUP BY sg.id, sg.name")
    List<Map<String, Object>> groupStatusSummary(@Param("timeoutSeconds") int timeoutSeconds);
}
