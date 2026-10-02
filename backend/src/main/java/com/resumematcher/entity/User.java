package com.resumematcher.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an application user who can upload resumes and job descriptions.
 * Maps to the "users" table in MySQL.
 */
@Entity
@Table(name = "users") // "user" is a reserved keyword in MySQL, so we use "users"
public class User {

    @Id // marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL auto-increment
    private Long id;

    @Column(nullable = false, unique = true) // every username must be unique
    private String username;

    @Column(nullable = false) // stored as a BCrypt hash later, never plain text
    private String password;

    @Column(nullable = false) // role controls access: "ROLE_USER" or "ROLE_ADMIN"
    private String role;

    // One user can upload many resumes.
    // mappedBy = "user" → the Resume entity owns the FK column.
    // CascadeType.ALL → insert/update/delete propagates to child resumes.
    // orphanRemoval → removing a Resume from this list deletes it from DB.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes = new ArrayList<>();

    // One user can create many job descriptions (same cascade logic)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobDescription> jobDescriptions = new ArrayList<>();

    // JPA requires a public no-arg constructor to create instances via reflection
    public User() {
    }

    // Convenience constructor for creating a user in service code
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ── Getters and Setters (no Lombok – plain Java) ──────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Resume> getResumes() {
        return resumes;
    }

    public void setResumes(List<Resume> resumes) {
        this.resumes = resumes;
    }

    public List<JobDescription> getJobDescriptions() {
        return jobDescriptions;
    }

    public void setJobDescriptions(List<JobDescription> jobDescriptions) {
        this.jobDescriptions = jobDescriptions;
    }
}
