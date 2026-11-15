package org.damon.user.service;

import org.damon.user.domain.UserIdentity;

import java.time.LocalDateTime;

public interface IThirdPartyTokenRefresher {

    boolean supports(String provider);

    TokenRefreshResult refresh(UserIdentity identity);

    record TokenRefreshResult(String accessToken, String refreshToken, LocalDateTime expiresAt) {
    }
}
