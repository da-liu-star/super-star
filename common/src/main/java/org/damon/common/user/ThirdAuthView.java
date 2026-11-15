package org.damon.common.user;

import java.time.LocalDateTime;

public record ThirdAuthView(Long id,
                            String provider,
                            String openid,
                            String unionid,
                            String nickname,
                            String avatarUrl,
                            String scope,
                            LocalDateTime expiresAt,
                            LocalDateTime createTime) {
}
