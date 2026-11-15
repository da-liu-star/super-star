package org.damon.oauth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.UserLoginResult;
import org.damon.oauth.service.ILoginSessionService;
import org.damon.oauth.service.IThirdAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;
    private final IThirdAuthService thirdAuthService;
    private final ILoginSessionService loginSessionService;
    private final ObjectMapper objectMapper;

    public OAuth2LoginSuccessHandler(
                                     ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
                                     IThirdAuthService thirdAuthService,
                                     ILoginSessionService loginSessionService,
                                     ObjectMapper objectMapper) {
        this.authorizedClientServiceProvider = authorizedClientServiceProvider;
        this.thirdAuthService = thirdAuthService;
        this.loginSessionService = loginSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oauthUser = oauthToken.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();

        String openid = firstNonBlank(attributes, "id", "sub", "openid");
        String nickname = firstNonBlank(attributes, "name", "nickname", "login");
        String avatarUrl = firstNonBlank(attributes, "avatar_url", "avatarUrl", "picture", "avatar");
        String rawProfile = serialize(attributes);

        OAuth2AuthorizedClientService authorizedClientService =
                authorizedClientServiceProvider.getIfAvailable();
        OAuth2AuthorizedClient client = authorizedClientService == null
                ? null
                : authorizedClientService.loadAuthorizedClient(provider, oauthToken.getName());
        String accessToken = null;
        String refreshToken = null;
        LocalDateTime expiresAt = null;
        if (client != null) {
            OAuth2AccessToken token = client.getAccessToken();
            if (token != null) {
                accessToken = token.getTokenValue();
                if (token.getExpiresAt() != null) {
                    expiresAt = LocalDateTime.ofInstant(token.getExpiresAt(), ZoneId.systemDefault());
                }
            }
            if (client.getRefreshToken() != null) {
                refreshToken = client.getRefreshToken().getTokenValue();
            }
        }

        UserLoginResult result = thirdAuthService.login(new SocialLoginRequest(
                provider,
                openid,
                true,
                accessToken,
                refreshToken,
                expiresAt == null ? null : expiresAt.toString(),
                null,
                null,
                nickname,
                avatarUrl,
                rawProfile));
        loginSessionService.authenticate(result);
        response.sendRedirect("/api/user/me");
    }

    private String serialize(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String firstNonBlank(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
