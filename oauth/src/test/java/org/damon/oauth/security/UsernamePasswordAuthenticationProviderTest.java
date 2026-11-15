package org.damon.oauth.security;

import org.damon.common.user.UserAuth;
import org.damon.oauth.service.IUserRemoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernamePasswordAuthenticationProviderTest {

    @Mock
    private IUserRemoteService userRemoteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsernamePasswordAuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        authenticationProvider =
                new UsernamePasswordAuthenticationProvider(userRemoteService, passwordEncoder);
    }

    @Test
    void authenticatesUsernameAndPasswordWithConfiguredAuthorities() {
        when(userRemoteService.findAuthByUsername("admin"))
                .thenReturn(Optional.of(user("admin", "encoded-password", "ROLE_USER, ROLE_ADMIN", 1)));
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("admin", "123456"));

        assertTrue(authentication.isAuthenticated());
        assertEquals("admin", authentication.getName());
        assertNull(authentication.getCredentials());
        assertEquals(
                List.of("ROLE_ADMIN", "ROLE_USER"),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted()
                        .toList());
        verify(passwordEncoder).matches("123456", "encoded-password");
    }

    @Test
    void rejectsIncorrectPassword() {
        when(userRemoteService.findAuthByUsername("admin"))
                .thenReturn(Optional.of(user("admin", "encoded-password", "ROLE_USER", 1)));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("admin", "wrong")));
    }

    @Test
    void rejectsUnknownUser() {
        when(userRemoteService.findAuthByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("missing", "123456")));
    }

    @Test
    void rejectsDisabledUser() {
        when(userRemoteService.findAuthByUsername("disabled"))
                .thenReturn(Optional.of(user("disabled", "encoded-password", "ROLE_USER", 0)));

        assertThrows(DisabledException.class, () -> authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("disabled", "123456")));
    }

    @Test
    void supportsUsernamePasswordAuthentication() {
        assertTrue(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    }

    private UserAuth user(String username, String password, String authorities, int status) {
        return new UserAuth(1L, username, password, username, null, null, null, authorities, status);
    }
}
