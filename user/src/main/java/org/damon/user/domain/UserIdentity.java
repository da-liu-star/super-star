package org.damon.user.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_identity")
public class UserIdentity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    private String provider;
    private String openid;
    private String unionid;

    @TableField("access_token")
    private String accessToken;

    @TableField("refresh_token")
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String scope;
    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("raw_profile")
    private String rawProfile;

    @TableField(value = "create_time",
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    @TableField(value = "update_time",
            insertStrategy = FieldStrategy.NEVER,
            updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
