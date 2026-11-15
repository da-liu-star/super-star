package org.damon.user.controller;

import org.damon.common.ApiResult;
import org.damon.common.user.UserProfile;
import org.damon.user.service.IUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/me"})
    public ApiResult<Map<String, Object>> me(Authentication authentication) {
        UserProfile profile = userService.findProfileByUsername(authentication.getName())
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
