package dev.lamkin.servermanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import dev.lamkin.servermanager.user.Role;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/resources/**");
    }

    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**").permitAll()
                .anyRequest().hasRole(Role.USER.name()));
        http.formLogin(form -> form.loginPage("/login")
                .successHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                .failureHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN)));
        http.exceptionHandling(exceptionHandlingCustomizer -> exceptionHandlingCustomizer
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        http.csrf(csrf -> csrf.disable()); // TODO

        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
