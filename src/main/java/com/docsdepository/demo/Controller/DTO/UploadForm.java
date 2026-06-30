package com.docsdepository.demo.Controller.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UploadForm {

    private Integer documentClassificationId;
    private String title;
    private String description;
    private LocalDate dateCreated; 
    private String filename;
    private Long uploadedBy; // User ID
    private Integer intendedViewerGroup; 
    private String tags;
    private Integer customViewerGroupId;
    private LocalDateTime documentDate;
    private String dtsNumber;
    private Integer targetAreaId;
    private Integer targetOfficeId;
    private Integer targetDepartmentId;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
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
    public Integer getCustomViewerGroupId() {
        return customViewerGroupId;
    }
    
    public void setCustomViewerGroupId(Integer customViewerGroupId) {
        this.customViewerGroupId = customViewerGroupId;
    }
    public LocalDateTime getDocumentDate() {
        return documentDate;
    }
    
    public void setDocumentDate(LocalDateTime documentDate) {
        this.documentDate = documentDate;
    }

    public String getDtsNumber() {
        return dtsNumber;
    }

    public void setDtsNumber(String dtsNumber) {
        this.dtsNumber = dtsNumber;
    }

    public Integer getTargetAreaId() { return targetAreaId; }
    public void setTargetAreaId(Integer targetAreaId) { this.targetAreaId = targetAreaId; }

    public Integer getTargetOfficeId() { return targetOfficeId; }
    public void setTargetOfficeId(Integer targetOfficeId) { this.targetOfficeId = targetOfficeId; }

    public Integer getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Integer targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }
}