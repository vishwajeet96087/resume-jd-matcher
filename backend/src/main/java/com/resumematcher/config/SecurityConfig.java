package com.resumematcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central security configuration.
 *
 * @Configuration  – tells Spring this class defines beans.
 * @EnableWebSecurity – activates Spring Security's web filters.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt hashes passwords with a random salt each time, so even identical
     * passwords produce different hashes.  This bean is injected wherever
     * Spring needs to hash or verify a password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the entire HTTP security pipeline as a filter chain.
     *
     * How it works at runtime:
     * 1. Every HTTP request passes through this chain of servlet filters.
     * 2. authorizeHttpRequests decides which URLs are public vs protected.
     * 3. formLogin tells Spring to render our custom /login page instead
     *    of the default auto-generated one.
     * 4. logout invalidates the session and redirects to /login?logout.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── URL-level authorization rules (checked top-to-bottom) ──
            .authorizeHttpRequests(auth -> auth
                // Public pages: anyone can access login, register, and static assets
                .requestMatchers("/register", "/login", "/css/**", "/js/**").permitAll()
                // Only users with ROLE_ADMIN can access /admin/** URLs
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Everything else requires the user to be logged in
                .anyRequest().authenticated()
            )
            // ── Form-based login ──
            .formLogin(form -> form
                .loginPage("/login")                      // our custom Thymeleaf page
                .defaultSuccessUrl("/dashboard", true)     // go here after successful login
                .permitAll()                               // the login page itself must be public
            )
            // ── Logout ──
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")         // redirect after logout
                .permitAll()
            );

        return http.build();
    }
}
