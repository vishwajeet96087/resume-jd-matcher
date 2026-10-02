package com.resumematcher.repository;

import com.resumematcher.entity.AnalysisResult;
import com.resumematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Provides CRUD operations for AnalysisResult entities.
 */
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    // JPQL query: join through Resume to find all analysis results for a given user.
    // We use @Query because "findByResumeUser" would work but is less readable,
    // and this makes the intent explicit for interview discussions.
    @Query("SELECT a FROM AnalysisResult a WHERE a.resume.user = :user ORDER BY a.analyzedAt DESC")
    List<AnalysisResult> findByUser(@Param("user") User user);
}
