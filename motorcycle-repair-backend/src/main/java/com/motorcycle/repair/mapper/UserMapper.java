package com.motorcycle.repair.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.motorcycle.repair.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    User selectByUsernameIncludeDeleted(String username);

    @Update("UPDATE sys_user SET deleted = 0, password = #{password}, real_name = #{realName}, " +
            "phone = #{phone}, email = #{email}, role = #{role}, status = #{status}, verified = #{verified}, " +
            "skill = #{skill}, update_time = NOW() WHERE username = #{username} AND deleted = 1")
    int recoverDeletedUser(@Param("username") String username, @Param("password") String password,
                           @Param("realName") String realName, @Param("phone") String phone,
                           @Param("email") String email, @Param("role") Integer role,
                           @Param("status") Integer status, @Param("verified") Integer verified,
                           @Param("skill") String skill);
}
