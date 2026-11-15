package org.damon.common.user;

public record BindIdentityRequest(String provider,
                                   String openid,
                                   String unionid,
                                   String accessToken,
                                   String refreshToken,
                                   String expiresAt,
                                   String scope,
                                   String nickname,
                                   String avatarUrl,
                                   String rawProfile) {
}
