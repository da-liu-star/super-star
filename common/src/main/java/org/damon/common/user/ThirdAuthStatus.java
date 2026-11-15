package org.damon.common.user;

import java.time.LocalDateTime;

public record ThirdAuthStatus(String provider,
                              String openid,
                              boolean valid,
                              LocalDateTime expiresAt) {
}
