package com.docsdepository.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Integer tagId;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String tagName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "tags")
    @JsonIgnore  // 👈 ADD THIS - Don't serialize documents when serializing tags
    private Set<ImportableInformation> documents = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Tag() {}

    public Tag(String tagName) {
        this.tagName = tagName.toLowerCase().trim();
    }

    // Getters and Setters
    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName.toLowerCase().trim();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<ImportableInformation> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<ImportableInformation> documents) {
        this.documents = documents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return tagName != null && tagName.equals(tag.tagName);
    }

    @Override
    public int hashCode() {
        return tagName != null ? tagName.hashCode() : 0;
    }
}