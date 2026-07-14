package com.led.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.led.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM t_user WHERE wechat_openid = #{openid}")
    User selectByOpenid(String openid);
}
