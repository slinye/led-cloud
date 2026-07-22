package com.led.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {

    @Select("SELECT COUNT(*) FROM t_alarm_record WHERE status = 'unread'")
    long countUnread();

    @Select("SELECT * FROM t_alarm_record ORDER BY created_at DESC LIMIT #{limit}")
    List<AlarmRecord> selectLatest(int limit);
}
