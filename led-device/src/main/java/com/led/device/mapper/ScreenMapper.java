package com.led.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Screen;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScreenMapper extends BaseMapper<Screen> {
    @Select("SELECT * FROM t_screen WHERE mqtt_client_id = #{mqttClientId}")
    Screen selectByMqttClientId(String mqttClientId);

    /** 真正在线的屏幕（状态=online 且 60秒内有心跳） */
    @Select("SELECT * FROM t_screen WHERE status = 'online' AND last_heartbeat >= DATE_SUB(NOW(), INTERVAL 60 SECOND)")
    List<Screen> selectOnlineScreens();

    /** 心跳超时的在线屏幕（状态=online 但超过60秒无心跳，需要标记为离线） */
    @Select("SELECT * FROM t_screen WHERE status = 'online' AND (last_heartbeat < DATE_SUB(NOW(), INTERVAL 60 SECOND) OR last_heartbeat IS NULL)")
    List<Screen> selectTimedOutScreens();
}
