package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfuture.currency.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE user_id = #{userId} LIMIT 1")
    User findByUserId(@Param("userId") String userId);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE user_id = #{userId}")
    boolean existsByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM users WHERE user_id IS NULL")
    List<User> findByUserIdIsNull();
}
