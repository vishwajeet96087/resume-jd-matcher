package com.resumematcher.service;

import com.resumematcher.entity.User;
import com.resumematcher.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges our User entity with Spring Security's authentication system.
 *
 * Spring Security doesn't know how our database looks.  It only knows
 * the UserDetailsService interface.  By implementing loadUserByUsername(),
 * we tell Spring: "here is how to look up a user and their role."
 *
 * At login time the flow is:
 *   1. User submits username + password on the form.
 *   2. Spring calls loadUserByUsername(username) → gets UserDetails.
 *   3. Spring compares the submitted password (after BCrypt hashing)
 *      with the stored hash from UserDetails.getPassword().
 *   4. If they match → authenticated.  If not → error.
 */
@Service // registers this class as a Spring-managed bean
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Constructor injection: Spring auto-injects UserRepository here.
    // Preferred over @Autowired on a field because it makes dependencies
    // explicit and the class easier to unit-test.
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up our custom User entity from MySQL
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // Convert our User entity into Spring Security's UserDetails object.
        // .roles("USER") automatically creates authority "ROLE_USER",
        // which matches hasRole("USER") in SecurityConfig.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())   // already BCrypt-hashed in DB
                .roles(user.getRole())           // "USER" or "ADMIN" (no ROLE_ prefix needed here)
                .build();
    }
}
