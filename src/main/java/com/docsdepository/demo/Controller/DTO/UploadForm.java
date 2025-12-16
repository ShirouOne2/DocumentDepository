package com.docsdepository.demo.Controller.DTO;

public class UploadForm {

    private Long documentClassificationId;
    private String title;
    private String description;
    private String dateCreated;
    private String filename;
    private Long uploadedBy; // User ID
    private String intendedViewerGroup; // New field

    // Getters and setters
    public Long getDocumentClassificationId() {
        return documentClassificationId;
    }

    public void setDocumentClassificationId(Long documentClassificationId) {
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

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
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

    public String getIntendedViewerGroup() {
        return intendedViewerGroup;
    }

    public void setIntendedViewerGroup(String intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }
}