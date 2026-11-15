package org.damon.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.damon.user.domain.UserIdentity;

import java.util.List;

@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentity> {

    @Select("""
            SELECT *
              FROM user_identity
             WHERE provider = #{provider}
               AND openid = #{openid}
             LIMIT 1
            """)
    UserIdentity selectByProviderAndOpenid(@Param("provider") String provider,
                                           @Param("openid") String openid);

    @Select("""
            SELECT *
              FROM user_identity
             WHERE user_id = #{userId}
             ORDER BY id
            """)
    List<UserIdentity> selectByUserId(@Param("userId") Long userId);
}
