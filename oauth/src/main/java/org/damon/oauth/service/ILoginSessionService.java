package org.damon.oauth.service;

import org.damon.common.user.UserLoginResult;

public interface ILoginSessionService {

    void authenticate(UserLoginResult loginResult);
}
