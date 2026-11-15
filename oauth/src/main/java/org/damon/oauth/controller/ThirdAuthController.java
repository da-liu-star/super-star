package org.damon.oauth.controller;

import org.damon.common.ApiResult;
import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserLoginResult;
import org.damon.oauth.service.ILoginSessionService;
import org.damon.oauth.service.IThirdAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/third-auth")
public class ThirdAuthController {

    private final IThirdAuthService thirdAuthService;
    private final ILoginSessionService loginSessionService;

    public ThirdAuthController(IThirdAuthService thirdAuthService,
                               ILoginSessionService loginSessionService) {
        this.thirdAuthService = thirdAuthService;
        this.loginSessionService = loginSessionService;
    }

    @PostMapping("/bind")
    public ApiResult<ThirdAuthView> bind(Authentication authentication,
                                         @RequestBody BindIdentityRequest request) {
        return ApiResult.ok(thirdAuthService.bind(authentication.getName(), request));
    }

    @PostMapping("/login")
    public ApiResult<UserLoginResult> login(@RequestBody SocialLoginRequest request) {
        UserLoginResult result = thirdAuthService.login(request);
        loginSessionService.authenticate(result);
        return ApiResult.ok(result);
    }

    @GetMapping("/accounts")
    public ApiResult<List<ThirdAuthView>> accounts(Authentication authentication) {
        return ApiResult.ok(thirdAuthService.accounts(authentication.getName()));
    }

    @DeleteMapping("/{provider}")
    public ApiResult<Void> unbind(Authentication authentication,
                                  @PathVariable String provider) {
        thirdAuthService.unbind(authentication.getName(), provider);
        return ApiResult.ok(null);
    }

    @GetMapping("/{provider}/status")
    public ApiResult<ThirdAuthStatus> status(Authentication authentication,
                                             @PathVariable String provider) {
        return ApiResult.ok(thirdAuthService.status(authentication.getName(), provider));
    }
}
