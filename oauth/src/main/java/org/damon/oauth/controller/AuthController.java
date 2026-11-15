package org.damon.oauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.damon.common.ApiResult;
import org.damon.common.BizException;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.UserProfile;
import org.damon.oauth.service.IUserRemoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final IUserRemoteService userRemoteService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          IUserRemoteService userRemoteService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRemoteService = userRemoteService;
    }

    @PostMapping("/register")
    public ApiResult<AuthUserView> register(@RequestBody RegisterUserRequest request) {
        return ApiResult.ok(toView(userRemoteService.register(request)));
    }

    @PostMapping("/login")
    public ApiResult<AuthUserView> login(@RequestBody PasswordLoginRequest request,
                                         HttpServletRequest servletRequest,
                                         HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
            return ApiResult.ok(findView(authentication));
        } catch (AuthenticationException exception) {
            throw new BizException("Username or password is incorrect");
        }
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResult<AuthUserView>> session(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error(401, "Not authenticated"));
        }
        return ResponseEntity.ok(ApiResult.ok(findView(authentication)));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        return ApiResult.ok(null);
    }

    private AuthUserView findView(Authentication authentication) {
        UserProfile profile = userRemoteService.findProfileByUsername(authentication.getName())
                .orElseThrow(() -> new BizException("Authenticated user does not exist"));
        return toView(profile);
    }

    private AuthUserView toView(UserProfile profile) {
        List<String> authorities = profile.authorities() == null
                ? List.of()
                : Arrays.stream(profile.authorities().split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        return new AuthUserView(profile.id(), profile.username(), profile.nickname(),
                profile.email(), profile.avatarUrl(), authorities);
    }

    public record PasswordLoginRequest(String username, String password) {
    }

    public record AuthUserView(Long userId,
                               String username,
                               String nickname,
                               String email,
                               String avatarUrl,
                               List<String> authorities) {
    }
}
