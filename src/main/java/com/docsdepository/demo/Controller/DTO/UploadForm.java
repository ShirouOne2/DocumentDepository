package com.docsdepository.demo.Controller.DTO;

import java.time.LocalDate;

public class UploadForm {

    private Integer documentClassificationId;
    private String title;
    private String description;
    private LocalDate dateCreated; 
    private String filename;
    private Long uploadedBy; // User ID
    private Integer intendedViewerGroup; 

    // Getters and setters
    public Integer getDocumentClassificationId() {
        return documentClassificationId;
    }

    public void setDocumentClassificationId(Integer documentClassificationId) {
        this.documentClassificationId = documentClassificationId;
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

    public LocalDate getDateCreated() {
        return dateCreated;
    }
    
    public void setDateCreated(LocalDate dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Integer getIntendedViewerGroup() {
        return intendedViewerGroup;
    }
    
    public void setIntendedViewerGroup(Integer intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }
}