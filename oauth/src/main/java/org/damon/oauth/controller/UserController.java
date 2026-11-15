package org.damon.oauth.controller;

import org.damon.common.ApiResult;
import org.damon.common.user.UserProfile;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility facade for browser sessions created by the authorization server.
 * The canonical user API is exposed by the user service.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final IUserRemoteService userRemoteService;

    public UserController(IUserRemoteService userRemoteService) {
        this.userRemoteService = userRemoteService;
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(Authentication authentication) {
        UserProfile profile = userRemoteService.findProfileByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user does not exist"));
        List<String> authorities = profile.authorities() == null
                ? List.of()
                : Arrays.stream(profile.authorities().split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", profile.id());
        result.put("username", profile.username());
        result.put("authorities", authorities);
        result.put("nickname", profile.nickname());
        result.put("avatarUrl", profile.avatarUrl());
        result.put("email", profile.email());
        result.put("phone", profile.phone());
        return ApiResult.ok(result);
    }
}
