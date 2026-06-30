package com.docsdepository.demo.Controller.DTO;

import java.time.LocalDateTime;

public class CommentDTO {
    private Integer commentId;
    private String commentText;
    private String username;
    private LocalDateTime createdAt; 
    private Integer userId;
    

    // Constructors
    public CommentDTO() {}

    public CommentDTO(
        Integer commentId,
        String commentText,
        Integer userId,
        String username,
        LocalDateTime createdAt
    ) {
        this.commentId = commentId;
        this.commentText = commentText;
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {  
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {  
        this.createdAt = createdAt;
    }
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}