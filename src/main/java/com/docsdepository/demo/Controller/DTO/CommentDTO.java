package com.docsdepository.demo.Controller.DTO;

import java.time.LocalDateTime;

public class CommentDTO {
    private Integer commentId;  // ✅ Changed from 'id' to 'commentId'
    private String commentText;
    private String username;
    private LocalDateTime createdDate;

    // Constructors
    public CommentDTO() {}

    public CommentDTO(Integer commentId, String commentText, String username, LocalDateTime createdDate) {
        this.commentId = commentId;
        this.commentText = commentText;
        this.username = username;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}