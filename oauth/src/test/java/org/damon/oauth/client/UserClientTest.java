package org.damon.oauth.client;

import org.damon.common.RemoteServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class UserClientTest {

    @Test
    void convertsUserServiceHttpFailureToRemoteServiceException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserClient userClient = new UserClient(builder, "http://user-service", "test-token");

        server.expect(requestTo("http://user-service/internal/users/100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "test-token"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RemoteServiceException exception = assertThrows(
                RemoteServiceException.class,
                () -> userClient.findProfileById(100L));

        assertTrue(exception.getMessage().contains("503 SERVICE_UNAVAILABLE"));
        server.verify();
    }
}
