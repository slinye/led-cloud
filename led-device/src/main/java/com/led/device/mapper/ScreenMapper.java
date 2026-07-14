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

    @Select("SELECT * FROM t_screen WHERE status = 'online'")
    List<Screen> selectOnlineScreens();
}
