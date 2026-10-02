package com.resumematcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * A predefined job-role template used in "resume-only" analysis.
 *
 * Instead of comparing a resume against a user-supplied JD, we compare it
 * against each RoleProfile's keyword list to find the best-fitting role.
 * Think of it as a built-in library of mini job descriptions.
 *
 * Example row:
 *   roleName = "Java Developer"
 *   keywords = "java, spring, hibernate, jdbc, maven, sql, rest, api, ..."
 *   description = "Backend development using Java and Spring ecosystem"
 */
@Entity
@Table(name = "role_profiles")
public class RoleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-readable name shown in the UI, e.g. "Full Stack Developer"
    @Column(nullable = false, unique = true)
    private String roleName;

    // Comma-separated keywords that define this role.
    // Stored as TEXT because the list can grow; parsed at runtime by splitting on commas.
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String keywords;

    // Short description of the role (shown in results for context)
    private String description;

    public RoleProfile() {
    }

    public RoleProfile(String roleName, String keywords, String description) {
        this.roleName = roleName;
        this.keywords = keywords;
        this.description = description;
    }

    // ── Getters and Setters ───────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
