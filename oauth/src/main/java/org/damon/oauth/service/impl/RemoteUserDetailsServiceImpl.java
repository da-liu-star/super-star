package org.damon.oauth.service.impl;

import org.damon.oauth.service.IRemoteUserDetailsService;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RemoteUserDetailsServiceImpl implements IRemoteUserDetailsService {

    private final IUserRemoteService userRemoteService;

    public RemoteUserDetailsServiceImpl(IUserRemoteService userRemoteService) {
        this.userRemoteService = userRemoteService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRemoteService.findAuthByUsername(username)
                .map(user -> User
                        .withUsername(user.username())
                        .password(user.password())
                        .authorities(parseAuthorities(user.authorities()))
                        .disabled(user.status() == null || user.status() != 1)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User does not exist: " + username));
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
