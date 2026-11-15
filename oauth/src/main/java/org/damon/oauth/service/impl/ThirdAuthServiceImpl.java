package org.damon.oauth.service.impl;

import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.damon.oauth.service.IThirdAuthService;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThirdAuthServiceImpl implements IThirdAuthService {

    private final IUserRemoteService userRemoteService;

    public ThirdAuthServiceImpl(IUserRemoteService userRemoteService) {
        this.userRemoteService = userRemoteService;
    }

    @Override
    public ThirdAuthView bind(String username, BindIdentityRequest request) {
        return userRemoteService.bind(currentUserId(username), request);
    }

    @Override
    public UserLoginResult login(SocialLoginRequest request) {
        return userRemoteService.socialLogin(request);
    }

    @Override
    public List<ThirdAuthView> accounts(String username) {
        return userRemoteService.listIdentities(currentUserId(username));
    }

    @Override
    public void unbind(String username, String provider) {
        userRemoteService.unbind(currentUserId(username), provider);
    }

    @Override
    public ThirdAuthStatus status(String username, String provider) {
        return userRemoteService.tokenStatus(currentUserId(username), provider);
    }

    private Long currentUserId(String username) {
        UserProfile profile = userRemoteService.findProfileByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user does not exist"));
        return profile.id();
    }
}
