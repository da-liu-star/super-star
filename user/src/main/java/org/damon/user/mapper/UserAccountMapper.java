package org.damon.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.damon.user.domain.UserAccount;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("""
            SELECT *
              FROM user_account
             WHERE username = #{username}
             LIMIT 1
            """)
    UserAccount selectByUsername(@Param("username") String username);
}
