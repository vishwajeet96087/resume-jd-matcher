package com.resumematcher.repository;

import com.resumematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA auto-generates the implementation at runtime.
 * JpaRepository<User, Long> gives us save(), findById(), findAll(), deleteById(), etc.
 * We only declare methods for queries that JPA can't infer from the entity alone.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data derives the query: SELECT * FROM users WHERE username = ?
    // Returns Optional because the user may not exist (avoids null checks)
    Optional<User> findByUsername(String username);
}
