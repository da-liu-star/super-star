package org.damon.common.user;

public record RegisterUserRequest(String username,
                                  String password,
                                  String nickname,
                                  String email) {
}
