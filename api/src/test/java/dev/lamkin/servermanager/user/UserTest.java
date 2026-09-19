package dev.lamkin.servermanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

class UserTest {

    @Test
    void constructorSetsProvidedFields() {
        User user = new User("alice", "hashed-password", Role.USER, true);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void constructorLeavesGeneratedFieldsUnset() {
        User user = new User("alice", "hashed-password", Role.USER, true);

        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void settersUpdateFields() {
        User user = new User("alice", "hashed-password", Role.USER, true);

        user.setUsername("bob");
        user.setPassword("new-hash");
        user.setRole(Role.USER);
        user.setEnabled(false);

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void settersUpdateGeneratedFields() {
        User user = new User("alice", "hashed-password", Role.USER, true);
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-02T00:00:00Z");

        user.setId(id);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Nested
    @DataJpaTest
    @Testcontainers
    @AutoConfigureTestDatabase(replace = Replace.NONE)
    class IntegrationTests {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

        @Autowired
        private TestEntityManager testEntityManager;

        @Test
        void persistingNewUserGeneratesId() {
            User user = new User("integration-id-" + UUID.randomUUID(), "hashed-password", Role.USER, true);

            User saved = testEntityManager.persistAndFlush(user);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        void persistingNewUserPopulatesTimestamps() {
            User user = new User("integration-ts-" + UUID.randomUUID(), "hashed-password", Role.USER, true);

            User saved = testEntityManager.persistAndFlush(user);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        void updatingUserChangesUpdatedAtButNotCreatedAt() {
            User user = new User("integration-upd-" + UUID.randomUUID(), "hashed-password", Role.USER, true);
            User saved = testEntityManager.persistAndFlush(user);
            Instant originalCreatedAt = saved.getCreatedAt();
            Instant originalUpdatedAt = saved.getUpdatedAt();

            saved.setEnabled(false);
            testEntityManager.flush();
            testEntityManager.clear();

            User reloaded = testEntityManager.find(User.class, saved.getId());

            assertThat(reloaded.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(reloaded.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        void enforcesNotNullUsername() {
            User user = new User(null, "hashed-password", Role.USER, true);

            assertThatThrownBy(() -> testEntityManager.persistAndFlush(user))
                    .isInstanceOf(PropertyValueException.class);
        }

        @Test
        void enforcesUniqueUsername() {
            String username = "duplicate-" + UUID.randomUUID();
            testEntityManager.persistAndFlush(new User(username, "hashed-password", Role.USER, true));
            User duplicate = new User(username, "another-hash", Role.USER, true);

            assertThatThrownBy(() -> testEntityManager.persistAndFlush(duplicate))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void storesRoleAsStringColumnValue() {
            User user = new User("integration-role-" + UUID.randomUUID(), "hashed-password", Role.USER, true);
            User saved = testEntityManager.persistAndFlush(user);

            Object rawRoleValue = testEntityManager.getEntityManager()
                    .createNativeQuery("SELECT role FROM users WHERE id = ?1")
                    .setParameter(1, saved.getId())
                    .getSingleResult();

            assertThat(rawRoleValue).isEqualTo("USER");
        }
    }
}
