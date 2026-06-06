package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid) ;

    /**
     * 插入用户数据
     *
     * @param user
     */
    void insert(User user);
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    Integer countByDate(LocalDateTime startTime, LocalDateTime endTime);
}
