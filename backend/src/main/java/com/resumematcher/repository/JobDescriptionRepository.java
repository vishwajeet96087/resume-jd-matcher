package com.resumematcher.repository;

import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Provides CRUD operations for JobDescription entities.
 */
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    // Derived query: SELECT * FROM job_descriptions WHERE user_id = ?
    // Shows all JDs that this user has saved
    List<JobDescription> findByUser(User user);
}
