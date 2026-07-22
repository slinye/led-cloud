package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Schedule;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /** 查询所有启用的调度（带节目名+屏幕名+clientId） */
    @Select("SELECT s.*, p.name as program_name, sc.name as screen_name, sc.mqtt_client_id " +
            "FROM t_schedule s " +
            "LEFT JOIN t_program p ON s.program_id = p.id " +
            "LEFT JOIN t_screen sc ON s.screen_id = sc.id " +
            "WHERE s.enabled = 1 " +
            "ORDER BY s.next_run_time ASC")
    @Results({
        @Result(property = "programName", column = "program_name"),
        @Result(property = "screenName", column = "screen_name"),
        @Result(property = "mqttClientId", column = "mqtt_client_id")
    })
    List<Schedule> selectEnabledSchedules();

    /** 分页查询所有调度 */
    @Select("SELECT s.*, p.name as program_name, sc.name as screen_name, sc.mqtt_client_id " +
            "FROM t_schedule s " +
            "LEFT JOIN t_program p ON s.program_id = p.id " +
            "LEFT JOIN t_screen sc ON s.screen_id = sc.id " +
            "ORDER BY s.updated_at DESC")
    @Results({
        @Result(property = "programName", column = "program_name"),
        @Result(property = "screenName", column = "screen_name"),
        @Result(property = "mqttClientId", column = "mqtt_client_id")
    })
    List<Schedule> selectAllWithDetails();
}
