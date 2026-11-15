package org.damon.oauth.service.impl;

import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserAuth;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.damon.oauth.client.UserClient;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserRemoteServiceImpl implements IUserRemoteService {

    private final UserClient userClient;

    public UserRemoteServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public Optional<UserAuth> findAuthByUsername(String username) {
        return userClient.findAuthByUsername(username);
    }

    @Override
    public Optional<UserProfile> findProfileByUsername(String username) {
        return userClient.findProfileByUsername(username);
    }

    @Override
    public UserProfile findProfileById(Long id) {
        return userClient.findProfileById(id);
    }

    @Override
    public UserProfile register(RegisterUserRequest request) {
        return userClient.register(request);
    }

    @Override
    public UserLoginResult socialLogin(SocialLoginRequest request) {
        return userClient.socialLogin(request);
    }

    @Override
    public ThirdAuthView bind(Long userId, BindIdentityRequest request) {
        return userClient.bind(userId, request);
    }

    @Override
    public List<ThirdAuthView> listIdentities(Long userId) {
        return userClient.listIdentities(userId);
    }

    @Override
    public void unbind(Long userId, String provider) {
        userClient.unbind(userId, provider);
    }

    @Override
    public ThirdAuthStatus tokenStatus(Long userId, String provider) {
        return userClient.tokenStatus(userId, provider);
    }
}
