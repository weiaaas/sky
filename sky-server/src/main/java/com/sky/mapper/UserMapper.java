package com.sky.mapper;

import com.sky.entity.DateAmount;
import com.sky.entity.User;
import com.sky.entity.UserAmount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */

    @Select("select * from user where openid =#{openid}")
    User getByopenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id=#{id}")
    User getById(Long id);

    List<UserAmount> countByDate(Map map);

    @Select("select count(*) from user where create_time<#{DateTime}")
    Long countUser(LocalDateTime DateTime);

    Integer countByMap(Map map);
}
