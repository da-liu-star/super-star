package org.damon.oauth.service;

import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserLoginResult;

import java.util.List;

public interface IThirdAuthService {

    ThirdAuthView bind(String username, BindIdentityRequest request);

    UserLoginResult login(SocialLoginRequest request);

    List<ThirdAuthView> accounts(String username);

    void unbind(String username, String provider);

    ThirdAuthStatus status(String username, String provider);
}
