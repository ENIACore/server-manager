package dev.lamkin.servermanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.lamkin.servermanager.user.Role;
import dev.lamkin.servermanager.user.User;
import dev.lamkin.servermanager.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class SecurityConfigTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser(String username, String rawPassword, boolean enabled) {
        return userRepository.saveAndFlush(
                new User(username, passwordEncoder.encode(rawPassword), Role.USER, enabled));
    }

    @Test
    void unauthenticatedRequestToProtectedPathIsUnauthorized() {
        assertThat(mockMvc.get().uri("/protected"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicPathDoesNotRequireAuthentication() {
        assertThat(mockMvc.get().uri("/public/anything"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void ignoredResourcesPathBypassesSecurity() {
        assertThat(mockMvc.get().uri("/resources/anything"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void loginWithValidCredentialsSucceeds() {
        String username = "login-ok-" + UUID.randomUUID();
        createUser(username, "secret", true);

        assertThat(mockMvc.post().uri("/login")
                .param("username", username)
                .param("password", "secret"))
                .hasStatusOk();
    }

    @Test
    void loginWithWrongPasswordIsForbidden() {
        String username = "login-bad-" + UUID.randomUUID();
        createUser(username, "secret", true);

        assertThat(mockMvc.post().uri("/login")
                .param("username", username)
                .param("password", "wrong"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginWithUnknownUsernameIsForbidden() {
        assertThat(mockMvc.post().uri("/login")
                .param("username", "unknown-" + UUID.randomUUID())
                .param("password", "secret"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginWithDisabledUserIsForbidden() {
        String username = "login-disabled-" + UUID.randomUUID();
        createUser(username, "secret", false);

        assertThat(mockMvc.post().uri("/login")
                .param("username", username)
                .param("password", "secret"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void authenticatedSessionCanReachProtectedPath() {
        String username = "login-session-" + UUID.randomUUID();
        createUser(username, "secret", true);
        MockHttpSession session = new MockHttpSession();

        assertThat(mockMvc.post().uri("/login")
                .param("username", username)
                .param("password", "secret")
                .session(session))
                .hasStatusOk();

        assertThat(mockMvc.get().uri("/protected").session(session))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
