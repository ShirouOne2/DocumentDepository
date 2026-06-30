package com.docsdepository.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_comments")
public class DocumentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private ImportableInformation document;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "created_at") 
    private LocalDateTime createdAt;  

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();  
        if (isActive == null) {
            isActive = true;
        }
    }

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Constructors
    public DocumentComment() {}

    public DocumentComment(ImportableInformation document, Users user, String commentText) {
        this.document = document;
        this.user = user;
        this.commentText = commentText;
    }

    // Getters and Setters
    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public ImportableInformation getDocument() {
        return document;
    }

    public void setDocument(ImportableInformation document) {
        this.document = document;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public LocalDateTime getCreatedAt() {  
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {  
        this.createdAt = createdAt;
    }
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}