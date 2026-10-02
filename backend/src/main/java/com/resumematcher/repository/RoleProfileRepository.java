package com.resumematcher.repository;

import com.resumematcher.entity.RoleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides CRUD for RoleProfile entities.
 * count() is inherited from JpaRepository — used by DataSeeder to check
 * if the table is already populated.
 */
public interface RoleProfileRepository extends JpaRepository<RoleProfile, Long> {
    // No custom queries needed — findAll() and count() are inherited.
}
