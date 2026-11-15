package org.damon.common.user;

import java.util.List;

public record UserLoginResult(Long userId,
                              String username,
                              String nickname,
                              String avatarUrl,
                              List<ThirdAuthView> thirdAuths) {
}
