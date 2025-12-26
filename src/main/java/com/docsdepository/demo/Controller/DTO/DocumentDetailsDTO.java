package com.docsdepository.demo.Controller.DTO;

import com.docsdepository.demo.Entity.DocumentComment;
import java.time.LocalDateTime;
import java.util.List;

public class DocumentDetailsDTO {
    private Integer documentId;
    private String title;
    private String description;
    private String filename;
    private String uploadedBy;
    private LocalDateTime dateCreated;
    private String documentClassification;
    private String intendedViewerGroup;
    private List<DocumentComment> comments;
    private Integer commentCount;
    private LocalDateTime uploadDate;

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    // Getters and Setters
    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Integer documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDocumentClassification() {
        return documentClassification;
    }

    public void setDocumentClassification(String documentClassification) {
        this.documentClassification = documentClassification;
    }

    public String getIntendedViewerGroup() {
        return intendedViewerGroup;
    }

    public void setIntendedViewerGroup(String intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }

    public List<DocumentComment> getComments() {
        return comments;
    }

    public void setComments(List<DocumentComment> comments) {
        this.comments = comments;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}