package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.PlayLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlayLogMapper extends BaseMapper<PlayLog> {
    @Select("SELECT * FROM t_play_log WHERE screen_id = #{screenId} ORDER BY created_at DESC")
    List<PlayLog> selectByScreenId(Long screenId);
}
