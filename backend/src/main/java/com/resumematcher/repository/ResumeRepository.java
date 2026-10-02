package com.resumematcher.repository;

import com.resumematcher.entity.Resume;
import com.resumematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Provides CRUD operations for Resume entities.
 * Custom query: find all resumes belonging to a specific user.
 */
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // Derived query: SELECT * FROM resumes WHERE user_id = ? ORDER BY uploaded_at DESC
    // Lets us show the user's resumes sorted newest-first on the dashboard
    List<Resume> findByUserOrderByUploadedAtDesc(User user);
}
