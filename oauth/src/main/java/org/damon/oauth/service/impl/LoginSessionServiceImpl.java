package org.damon.oauth.service.impl;

import org.damon.common.BizException;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.damon.oauth.service.ILoginSessionService;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class LoginSessionServiceImpl implements ILoginSessionService {

    private final IUserRemoteService userRemoteService;

    public LoginSessionServiceImpl(IUserRemoteService userRemoteService) {
        this.userRemoteService = userRemoteService;
    }

    @Override
    public void authenticate(UserLoginResult loginResult) {
        UserProfile profile = userRemoteService.findProfileById(loginResult.userId());
        if (profile == null || profile.status() == null || profile.status() != 1) {
            throw new BizException("User is disabled or does not exist");
        }
        User details = (User) User.withUsername(profile.username())
                .password("")
                .authorities(parseAuthorities(profile.authorities()))
                .disabled(profile.status() == null || profile.status() != 1)
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<GrantedAuthority> parseAuthorities(String authorities) {
        if (authorities == null || authorities.isBlank()) {
            return List.of();
        }
        return Arrays.stream(authorities.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
