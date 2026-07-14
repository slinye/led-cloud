package com.led.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {
    @Select("SELECT * FROM t_content ORDER BY created_at DESC")
    List<Content> selectAllOrderByTime();
}
