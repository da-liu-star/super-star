package org.damon.user.service.impl;

import org.damon.common.BizException;
import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserAuth;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.damon.user.domain.UserAccount;
import org.damon.user.domain.UserIdentity;
import org.damon.user.mapper.UserAccountMapper;
import org.damon.user.mapper.UserIdentityMapper;
import org.damon.user.service.IThirdPartyTokenRefresher;
import org.damon.user.service.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    private final UserAccountMapper accountMapper;
    private final UserIdentityMapper identityMapper;
    private final PasswordEncoder passwordEncoder;
    private final List<IThirdPartyTokenRefresher> tokenRefreshers;

    public UserServiceImpl(UserAccountMapper accountMapper,
                       UserIdentityMapper identityMapper,
                       PasswordEncoder passwordEncoder,
                       List<IThirdPartyTokenRefresher> tokenRefreshers) {
        this.accountMapper = accountMapper;
        this.identityMapper = identityMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenRefreshers = tokenRefreshers;
    }

    @Override
    public Optional<UserAuth> findAuthByUsername(String username) {
        return Optional.ofNullable(accountMapper.selectByUsername(username)).map(this::toAuth);
    }

    @Override
    public Optional<UserProfile> findProfileByUsername(String username) {
        return Optional.ofNullable(accountMapper.selectByUsername(username)).map(this::toProfile);
    }

    @Override
    public Optional<UserProfile> findProfileById(Long id) {
        return Optional.ofNullable(accountMapper.selectById(id)).map(this::toProfile);
    }

    @Override
    @Transactional
    public UserProfile register(RegisterUserRequest request) {
        String username = normalizeUsername(request.username());
        validateRegistration(request, username);

        if (accountMapper.selectByUsername(username) != null) {
            throw new BizException("Username is already registered");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setNickname(hasText(request.nickname()) ? request.nickname().trim() : username);
        account.setEmail(hasText(request.email()) ? request.email().trim() : null);
        account.setAuthorities("ROLE_USER");
        account.setStatus(1);
        accountMapper.insert(account);
        return toProfile(account);
    }

    @Override
    @Transactional
    public UserLoginResult socialLogin(SocialLoginRequest request) {
        validateIdentity(request.provider(), request.openid());

        UserIdentity identity = identityMapper
                .selectByProviderAndOpenid(request.provider(), request.openid());
        UserAccount account;

        if (identity == null) {
            if (!Boolean.TRUE.equals(request.createIfAbsent())) {
                throw new BizException("The third-party account is not bound to a user");
            }
            account = createThirdPartyUser(request.provider(), request.openid(),
                    request.nickname(), request.avatarUrl());
            identity = new UserIdentity();
            identity.setUserId(account.getId());
            identity.setProvider(request.provider());
            identity.setOpenid(request.openid());
        } else {
            account = Optional.ofNullable(accountMapper.selectById(identity.getUserId()))
                    .orElseThrow(() -> new BizException("The bound user does not exist"));
        }

        apply(identity, request);
        if (identity.getId() == null) {
            identityMapper.insert(identity);
        } else {
            identityMapper.updateById(identity);
        }

        ensureEnabled(account);
        return new UserLoginResult(account.getId(), account.getUsername(), account.getNickname(),
                account.getAvatarUrl(), listByUserId(account.getId()));
    }

    @Override
    @Transactional
    public ThirdAuthView bind(Long userId, BindIdentityRequest request) {
        Optional.ofNullable(accountMapper.selectById(userId))
                .orElseThrow(() -> new BizException("User does not exist"));
        validateIdentity(request.provider(), request.openid());

        UserIdentity identity = identityMapper
                .selectByProviderAndOpenid(request.provider(), request.openid());
        if (identity == null) {
            identity = new UserIdentity();
            identity.setUserId(userId);
            identity.setProvider(request.provider());
            identity.setOpenid(request.openid());
        } else if (!identity.getUserId().equals(userId)) {
            throw new BizException("The third-party account is already bound to another user");
        }

        apply(identity, request);
        if (identity.getId() == null) {
            identityMapper.insert(identity);
        } else {
            identityMapper.updateById(identity);
        }
        return toView(identity);
    }

    @Override
    public List<ThirdAuthView> listByUserId(Long userId) {
        return identityMapper.selectByUserId(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public void unbind(Long userId, String provider) {
        List<UserIdentity> identities = identityMapper.selectByUserId(userId).stream()
                .filter(identity -> identity.getProvider().equals(provider))
                .toList();
        if (identities.isEmpty()) {
            throw new BizException("Provider is not bound: " + provider);
        }
        identities.forEach(identity -> identityMapper.deleteById(identity.getId()));
    }

    @Override
    @Transactional
    public ThirdAuthStatus getTokenStatus(Long userId, String provider) {
        UserIdentity identity = identityMapper.selectByUserId(userId).stream()
                .filter(item -> item.getProvider().equals(provider))
                .findFirst()
                .orElseThrow(() -> new BizException("Provider is not bound: " + provider));

        if (identity.getExpiresAt() != null
                && identity.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))
                && hasText(identity.getRefreshToken())) {
            refreshAccessToken(identity);
        }
        if (identity.getExpiresAt() != null && identity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("The access token has expired and cannot be refreshed: " + provider);
        }

        boolean valid = hasText(identity.getAccessToken())
                && (identity.getExpiresAt() == null || identity.getExpiresAt().isAfter(LocalDateTime.now()));
        return new ThirdAuthStatus(identity.getProvider(), identity.getOpenid(), valid, identity.getExpiresAt());
    }

    private UserAccount createThirdPartyUser(String provider, String openid,
                                             String nickname, String avatarUrl) {
        String username = uniqueUsername(provider + "_" + openid);
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setNickname(hasText(nickname) ? nickname : username);
        account.setAvatarUrl(avatarUrl);
        account.setAuthorities("ROLE_USER");
        account.setStatus(1);
        accountMapper.insert(account);
        return account;
    }

    private String uniqueUsername(String base) {
        String normalized = base.replaceAll("[^A-Za-z0-9_-]", "_");
        if (normalized.isBlank()) {
            normalized = "user";
        }
        normalized = normalized.substring(0, Math.min(normalized.length(), 56));

        String candidate = normalized;
        int suffix = 1;
        while (accountMapper.selectByUsername(candidate) != null) {
            String tail = "_" + suffix++;
            candidate = normalized.substring(0, Math.min(normalized.length(), 64 - tail.length())) + tail;
        }
        return candidate;
    }

    private void apply(UserIdentity identity, BindIdentityRequest request) {
        apply(identity, request.unionid(), request.accessToken(), request.refreshToken(),
                request.expiresAt(), request.scope(), request.nickname(), request.avatarUrl(),
                request.rawProfile());
    }

    private void apply(UserIdentity identity, SocialLoginRequest request) {
        apply(identity, request.unionid(), request.accessToken(), request.refreshToken(),
                request.expiresAt(), request.scope(), request.nickname(), request.avatarUrl(),
                request.rawProfile());
    }

    private void apply(UserIdentity identity, String unionid, String accessToken,
                       String refreshToken, String expiresAt, String scope,
                       String nickname, String avatarUrl, String rawProfile) {
        if (unionid != null) {
            identity.setUnionid(unionid);
        }
        if (accessToken != null) {
            identity.setAccessToken(accessToken);
        }
        if (refreshToken != null) {
            identity.setRefreshToken(refreshToken);
        }
        if (expiresAt != null && !expiresAt.isBlank()) {
            LocalDateTime parsed = parseTime(expiresAt);
            if (parsed != null) {
                identity.setExpiresAt(parsed);
            }
        }
        if (scope != null) {
            identity.setScope(scope);
        }
        if (nickname != null) {
            identity.setNickname(nickname);
        }
        if (avatarUrl != null) {
            identity.setAvatarUrl(avatarUrl);
        }
        if (rawProfile != null) {
            identity.setRawProfile(rawProfile);
        }
    }

    private void refreshAccessToken(UserIdentity identity) {
        IThirdPartyTokenRefresher refresher = tokenRefreshers.stream()
                .filter(item -> item.supports(identity.getProvider()))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        "Token refresh is not implemented for provider: " + identity.getProvider()));
        IThirdPartyTokenRefresher.TokenRefreshResult result = refresher.refresh(identity);
        identity.setAccessToken(result.accessToken());
        identity.setRefreshToken(result.refreshToken());
        identity.setExpiresAt(result.expiresAt());
        identityMapper.updateById(identity);
    }

    private void validateIdentity(String provider, String openid) {
        if (!hasText(provider) || !hasText(openid)) {
            throw new BizException("Provider and openid are required");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private void validateRegistration(RegisterUserRequest request, String username) {
        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new BizException("Username must be 3-20 letters, numbers, or underscores");
        }
        if (request.password() == null || request.password().length() < 6
                || request.password().length() > 64) {
            throw new BizException("Password must be 6-64 characters");
        }
        if (hasText(request.nickname()) && request.nickname().trim().length() > 64) {
            throw new BizException("Nickname must be at most 64 characters");
        }
        if (hasText(request.email())
                && (request.email().trim().length() > 128
                || !request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new BizException("Email format is invalid");
        }
    }

    private void ensureEnabled(UserAccount account) {
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BizException("User is disabled");
        }
    }

    private LocalDateTime parseTime(String value) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value.trim(), formatter);
            } catch (Exception ignored) {
                // Try the next supported format.
            }
        }
        try {
            return OffsetDateTime.parse(value.trim()).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private UserAuth toAuth(UserAccount account) {
        return new UserAuth(account.getId(), account.getUsername(), account.getPassword(),
                account.getNickname(), account.getAvatarUrl(), account.getEmail(),
                account.getPhone(), account.getAuthorities(), account.getStatus());
    }

    private UserProfile toProfile(UserAccount account) {
        return new UserProfile(account.getId(), account.getUsername(), account.getNickname(),
                account.getAvatarUrl(), account.getEmail(), account.getPhone(),
                account.getAuthorities(), account.getStatus());
    }

    private ThirdAuthView toView(UserIdentity identity) {
        return new ThirdAuthView(identity.getId(), identity.getProvider(), identity.getOpenid(),
                identity.getUnionid(), identity.getNickname(), identity.getAvatarUrl(),
                identity.getScope(), identity.getExpiresAt(), identity.getCreateTime());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
