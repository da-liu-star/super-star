package org.damon.common.user;

public record SocialLoginRequest(String provider,
                                 String openid,
                                 Boolean createIfAbsent,
                                 String accessToken,
                                 String refreshToken,
                                 String expiresAt,
                                 String scope,
                                 String unionid,
                                 String nickname,
                                 String avatarUrl,
                                 String rawProfile) {
}
