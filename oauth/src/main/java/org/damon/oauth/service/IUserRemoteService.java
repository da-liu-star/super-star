package org.damon.oauth.service;

import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserAuth;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;

import java.util.List;
import java.util.Optional;

public interface IUserRemoteService {

    Optional<UserAuth> findAuthByUsername(String username);

    Optional<UserProfile> findProfileByUsername(String username);

    UserProfile findProfileById(Long id);

    UserProfile register(RegisterUserRequest request);

    UserLoginResult socialLogin(SocialLoginRequest request);

    ThirdAuthView bind(Long userId, BindIdentityRequest request);

    List<ThirdAuthView> listIdentities(Long userId);

    void unbind(Long userId, String provider);

    ThirdAuthStatus tokenStatus(Long userId, String provider);
}
