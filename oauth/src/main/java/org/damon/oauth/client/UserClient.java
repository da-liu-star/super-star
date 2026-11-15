package org.damon.oauth.client;

import lombok.Data;
import org.damon.common.BizException;
import org.damon.common.RemoteServiceException;
import org.damon.common.user.BindIdentityRequest;
import org.damon.common.user.RegisterUserRequest;
import org.damon.common.user.SocialLoginRequest;
import org.damon.common.user.ThirdAuthStatus;
import org.damon.common.user.ThirdAuthView;
import org.damon.common.user.UserAuth;
import org.damon.common.user.UserLoginResult;
import org.damon.common.user.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
public class UserClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final RestClient restClient;
    private final String internalToken;

    @Autowired
    public UserClient(@Value("${user-service.base-url}") String baseUrl,
                      @Value("${user-service.internal-token}") String internalToken) {
        this(RestClient.builder(), baseUrl, internalToken);
    }

    UserClient(RestClient.Builder restClientBuilder, String baseUrl, String internalToken) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        this.internalToken = internalToken;
    }

    public Optional<UserAuth> findAuthByUsername(String username) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/users/by-username/{username}")
                            .build(username))
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(UserAuth.class));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public Optional<UserProfile> findProfileByUsername(String username) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/users/by-username/{username}/profile")
                            .build(username))
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(UserProfile.class));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public UserProfile findProfileById(Long id) {
        try {
            return restClient.get()
                    .uri("/internal/users/{id}", id)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(UserProfile.class);
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public UserProfile register(RegisterUserRequest request) {
        try {
            return restClient.post()
                    .uri("/internal/users")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(request)
                    .retrieve()
                    .body(UserProfile.class);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new BizException("Registration data is invalid or the username is already registered");
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public UserLoginResult socialLogin(SocialLoginRequest request) {
        try {
            return restClient.post()
                    .uri("/internal/users/social-login")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(request)
                    .retrieve()
                    .body(UserLoginResult.class);
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public ThirdAuthView bind(Long userId, BindIdentityRequest request) {
        try {
            return restClient.post()
                    .uri("/internal/users/{id}/identities", userId)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(request)
                    .retrieve()
                    .body(ThirdAuthView.class);
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public List<ThirdAuthView> listIdentities(Long userId) {
        try {
            return restClient.get()
                    .uri("/internal/users/{id}/identities", userId)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public void unbind(Long userId, String provider) {
        try {
            restClient.delete()
                    .uri("/internal/users/{id}/identities/{provider}", userId, provider)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    public ThirdAuthStatus tokenStatus(Long userId, String provider) {
        try {
            return restClient.get()
                    .uri("/internal/users/{id}/identities/{provider}/status", userId, provider)
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(ThirdAuthStatus.class);
        } catch (RestClientResponseException exception) {
            throw remoteFailure(exception);
        } catch (RestClientException exception) {
            throw remoteFailure(exception);
        }
    }

    private RemoteServiceException remoteFailure(RestClientResponseException exception) {
        return new RemoteServiceException("User service request failed: " + exception.getStatusCode(), exception);
    }

    private RemoteServiceException remoteFailure(RestClientException exception) {
        return new RemoteServiceException("User service is unavailable", exception);
    }
}
