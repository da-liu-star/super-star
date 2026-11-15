package org.damon.user.service;

import org.damon.common.BizException;
import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.UserLoginResult;
import org.damon.user.domain.UserAccount;
import org.damon.user.domain.UserIdentity;
import org.damon.user.mapper.UserAccountMapper;
import org.damon.user.mapper.UserIdentityMapper;
import org.damon.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountMapper accountMapper;

    @Mock
    private UserIdentityMapper identityMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                accountMapper,
                identityMapper,
                passwordEncoder,
                List.of());
    }

    @Test
    void socialLoginCreatesUserAndIdentityWhenProviderIsNew() {
        when(identityMapper.selectByProviderAndOpenid("github", "42"))
                .thenReturn(null);
        when(accountMapper.selectByUsername("github_42"))
                .thenReturn(null);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");
        when(accountMapper.insert(any(UserAccount.class)))
                .thenAnswer(invocation -> {
                    UserAccount account = invocation.getArgument(0);
                    account.setId(100L);
                    return 1;
                });
        when(identityMapper.selectByUserId(100L))
                .thenAnswer(invocation -> List.of(identityRepositoryArgument()));
        doAnswer(invocation -> {
            UserIdentity identity = invocation.getArgument(0);
            identity.setId(200L);
            return 1;
        }).when(identityMapper).insert(any(UserIdentity.class));

        UserLoginResult result = userService.socialLogin(new SocialLoginRequest(
                "github",
                "42",
                true,
                "access-token",
                "refresh-token",
                "2030-01-01T00:00:00",
                "read:user",
                null,
                "Damon",
                "https://example.test/avatar.png",
                "{\"id\":42}"));

        assertEquals(100L, result.userId());
        assertEquals("github_42", result.username());
        verify(accountMapper).insert(any(UserAccount.class));
        verify(identityMapper).insert(any(UserIdentity.class));
    }

    @Test
    void registerCreatesAnEnabledPasswordAccount() {
        when(accountMapper.selectByUsername("damon_01")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(accountMapper.insert(any(UserAccount.class)))
                .thenAnswer(invocation -> {
                    UserAccount account = invocation.getArgument(0);
                    account.setId(101L);
                    return 1;
                });

        var profile = userService.register(new RegisterUserRequest(
                "damon_01",
                "123456",
                "Damon",
                "damon@example.com"));

        assertEquals(101L, profile.id());
        assertEquals("damon_01", profile.username());
        assertEquals("Damon", profile.nickname());
        assertEquals("ROLE_USER", profile.authorities());
        assertEquals(1, profile.status());
        verify(accountMapper).insert(any(UserAccount.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(accountMapper.selectByUsername("admin"))
                .thenReturn(enabledAccount(100L, "admin"));

        assertThrows(BizException.class, () -> userService.register(
                new RegisterUserRequest("admin", "123456", "Admin", null)));

        verify(accountMapper, never()).insert(any(UserAccount.class));
    }

    @Test
    void socialLoginUpdatesExistingIdentity() {
        UserAccount account = enabledAccount(100L, "admin");
        UserIdentity identity = identity(200L, 100L, "github", "42");
        when(identityMapper.selectByProviderAndOpenid("github", "42"))
                .thenReturn(identity);
        when(accountMapper.selectById(100L)).thenReturn(account);
        when(identityMapper.selectByUserId(100L)).thenReturn(List.of(identity));

        UserLoginResult result = userService.socialLogin(new SocialLoginRequest(
                "github",
                "42",
                false,
                "new-access-token",
                null,
                null,
                null,
                null,
                "Updated name",
                null,
                null));

        assertEquals(100L, result.userId());
        assertEquals("new-access-token", identity.getAccessToken());
        assertEquals("Updated name", identity.getNickname());
        verify(identityMapper).updateById(identity);
        verify(identityMapper, never()).insert(any(UserIdentity.class));
    }

    @Test
    void bindRejectsIdentityOwnedByAnotherUser() {
        UserIdentity identity = identity(200L, 999L, "github", "42");
        when(accountMapper.selectById(100L)).thenReturn(enabledAccount(100L, "admin"));
        when(identityMapper.selectByProviderAndOpenid("github", "42"))
                .thenReturn(identity);

        assertThrows(BizException.class, () -> userService.bind(
                100L,
                new BindIdentityRequest(
                        "github",
                        "42",
                        null,
                        "access-token",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        verify(identityMapper, never()).insert(any(UserIdentity.class));
        verify(identityMapper, never()).updateById(any(UserIdentity.class));
    }

    @Test
    void unbindDeletesOnlyTheSelectedProvider() {
        UserIdentity github = identity(200L, 100L, "github", "42");
        UserIdentity wechat = identity(201L, 100L, "wechat", "openid");
        when(identityMapper.selectByUserId(100L)).thenReturn(List.of(github, wechat));

        userService.unbind(100L, "github");

        verify(identityMapper).deleteById(200L);
        verify(identityMapper, never()).deleteById(eq(201L));
    }

    private UserAccount enabledAccount(Long id, String username) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setUsername(username);
        account.setNickname(username);
        account.setStatus(1);
        account.setAuthorities("ROLE_USER");
        return account;
    }

    private UserIdentity identity(Long id, Long userId, String provider, String openid) {
        UserIdentity identity = new UserIdentity();
        identity.setId(id);
        identity.setUserId(userId);
        identity.setProvider(provider);
        identity.setOpenid(openid);
        identity.setNickname("Original name");
        return identity;
    }

    private UserIdentity identityRepositoryArgument() {
        UserIdentity identity = identity(200L, 100L, "github", "42");
        identity.setNickname("Damon");
        return identity;
    }
}
