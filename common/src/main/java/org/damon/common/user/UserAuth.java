package org.damon.common.user;

public record UserAuth(Long id,
                       String username,
                       String password,
                       String nickname,
                       String avatarUrl,
                       String email,
                       String phone,
                       String authorities,
                       Integer status) {
}
