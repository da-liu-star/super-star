package org.damon.common.user;

public record UserProfile(Long id,
                          String username,
                          String nickname,
                          String avatarUrl,
                          String email,
                          String phone,
                          String authorities,
                          Integer status) {
}
