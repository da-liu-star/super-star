package org.damon.oauth.security;

import org.damon.common.user.UserAuth;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private final IUserRemoteService userRemoteService;
    private final PasswordEncoder passwordEncoder;

    public UsernamePasswordAuthenticationProvider(IUserRemoteService userRemoteService,
                                                   PasswordEncoder passwordEncoder) {
        this.userRemoteService = userRemoteService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();

        if (username == null || username.isBlank()
                || !(credentials instanceof String rawPassword)
                || rawPassword.isBlank()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserAuth user = userRemoteService.findAuthByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.status() == null || user.status() != 1) {
            throw new DisabledException("User is disabled");
        }

        if (!passwordEncoder.matches(rawPassword, user.password())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<GrantedAuthority> authorities = parseAuthorities(user.authorities());
        UserDetails principal = User.withUsername(user.username())
                .password("")
                .authorities(authorities)
                .build();

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
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
