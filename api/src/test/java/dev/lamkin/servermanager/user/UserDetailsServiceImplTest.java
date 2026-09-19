package dev.lamkin.servermanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailsServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl(userRepository);

    @Test
    void loadUserByUsernameReturnsUserDetailsForExistingEnabledUser() {
        User user = new User("alice", "hashed-password", Role.USER, true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameReturnsDisabledUserDetailsForDisabledUser() {
        User user = new User("bob", "hashed-password", Role.USER, false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("bob");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsernameThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
