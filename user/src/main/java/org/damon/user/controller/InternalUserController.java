package org.damon.user.controller;

import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserAuth;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.damon.user.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final IUserService userService;

    public InternalUserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserAuth> findAuth(@PathVariable String username) {
        return userService.findAuthByUsername(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}/profile")
    public ResponseEntity<UserProfile> findProfile(@PathVariable String username) {
        return userService.findProfileByUsername(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> findProfile(@PathVariable Long id) {
        return userService.findProfileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserProfile register(@RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @PostMapping("/social-login")
    public UserLoginResult socialLogin(@RequestBody SocialLoginRequest request) {
        return userService.socialLogin(request);
    }

    @PostMapping("/{id}/identities")
    public ThirdAuthView bind(@PathVariable Long id,
                              @RequestBody BindIdentityRequest request) {
        return userService.bind(id, request);
    }

    @GetMapping("/{id}/identities")
    public List<ThirdAuthView> identities(@PathVariable Long id) {
        return userService.listByUserId(id);
    }

    @DeleteMapping("/{id}/identities/{provider}")
    public ResponseEntity<Void> unbind(@PathVariable Long id,
                                       @PathVariable String provider) {
        userService.unbind(id, provider);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/identities/{provider}/status")
    public ThirdAuthStatus tokenStatus(@PathVariable Long id,
                                       @PathVariable String provider) {
        return userService.getTokenStatus(id, provider);
    }
}
