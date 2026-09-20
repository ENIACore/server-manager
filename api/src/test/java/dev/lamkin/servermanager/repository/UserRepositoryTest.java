package dev.lamkin.servermanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import dev.lamkin.servermanager.entity.Role;
import dev.lamkin.servermanager.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameReturnsUserWhenPresent() {
        String username = "repo-find-" + UUID.randomUUID();
        userRepository.saveAndFlush(new User(username, "hashed-password", Role.USER, true));

        Optional<User> found = userRepository.findByUsername(username);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(username);
    }

    @Test
    void findByUsernameReturnsEmptyWhenAbsent() {
        Optional<User> found = userRepository.findByUsername("does-not-exist-" + UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUsernameIsCaseSensitive() {
        String username = "Repo-Case-" + UUID.randomUUID();
        userRepository.saveAndFlush(new User(username, "hashed-password", Role.USER, true));

        Optional<User> found = userRepository.findByUsername(username.toLowerCase());

        assertThat(found).isEmpty();
    }
}
