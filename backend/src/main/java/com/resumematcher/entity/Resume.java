package com.resumematcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Stores metadata and extracted text of an uploaded resume.
 * The raw PDF/TXT file lives on disk; only the parsed text is kept in the DB
 * so we can run keyword matching without re-reading the file.
 */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // original file name, shown in the UI
    private String fileName;

    @Lob // tells JPA this is a large-object column
    @Column(columnDefinition = "LONGTEXT") // resume text can be thousands of words
    private String extractedText;

    @Column(nullable = false) // timestamp so we can sort "most recent" uploads
    private LocalDateTime uploadedAt;

    // Many resumes belong to one user.
    // FetchType.LAZY → the User object is loaded only when getUser() is called,
    // saving a JOIN when we only need resume data.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK column in the resumes table
    private User user;

    public Resume() {
    }

    public Resume(String fileName, String extractedText, LocalDateTime uploadedAt, User user) {
        this.fileName = fileName;
        this.extractedText = extractedText;
        this.uploadedAt = uploadedAt;
        this.user = user;
    }

    // ── Getters and Setters ───────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
