package com.docsdepository.demo.Controller.DTO;

import com.docsdepository.demo.Entity.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhanced DTO for document details with editing support
 * Includes both display values (names) and IDs for editing
 */
public class DocumentDetailsDTO {
    // Basic document info
    private Integer documentId;
    private String title;
    private String description;
    private String filename;
    private String uploadedBy;
    private Integer uploadedById;          // ← NEW: used by JS for ownership check
    private LocalDateTime dateCreated;
    private LocalDateTime uploadDate;

    // Classification - both name (for display) and ID (for editing)
    private String documentClassification;
    private Integer classificationId;

    // Viewer group - both name (for display) and ID (for editing)
    private String intendedViewerGroup;
    private Integer viewerGroupId;

    // Custom viewer group - both name and ID
    private Integer customGroupId;
    private String customGroupName;

    // Tags - list of tag names for easy frontend consumption
    private List<String> tagNames;

    // Comments
    private List<CommentDTO> comments;
    private Integer commentCount;

    // Current user (for permission checks)
    private Integer currentUserId;

    // ← NEW: whether the uploader has a department assigned.
    //   When false, the "Department" viewer-group option must be hidden in the edit form.
    private boolean uploaderHasDepartment;

    // DTS Number
    private String dtsNumber;

    // Target org — snapshotted at upload time, used for visibility
    private Integer targetAreaId;
    private String  targetAreaName;
    private Integer targetOfficeId;
    private String  targetOfficeName;
    private Integer targetDepartmentId;
    private String  targetDepartmentName;

    // Contract period — only populated when classification is a Contract type
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    // Constructors
    public DocumentDetailsDTO() {}

    // ─── Getters / Setters ───────────────────────────────────────────────

    public Integer getDocumentId() { return documentId; }
    public void setDocumentId(Integer documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    /** Numeric ID of the user who uploaded this document. */
    public Integer getUploadedById() { return uploadedById; }
    public void setUploadedById(Integer uploadedById) { this.uploadedById = uploadedById; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getDocumentClassification() { return documentClassification; }
    public void setDocumentClassification(String documentClassification) {
        this.documentClassification = documentClassification;
    }

    public Integer getClassificationId() { return classificationId; }
    public void setClassificationId(Integer classificationId) {
        this.classificationId = classificationId;
    }

    public String getIntendedViewerGroup() { return intendedViewerGroup; }
    public void setIntendedViewerGroup(String intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }

    public Integer getViewerGroupId() { return viewerGroupId; }
    public void setViewerGroupId(Integer viewerGroupId) { this.viewerGroupId = viewerGroupId; }

    public Integer getCustomGroupId() { return customGroupId; }
    public void setCustomGroupId(Integer customGroupId) { this.customGroupId = customGroupId; }

    public String getCustomGroupName() { return customGroupName; }
    public void setCustomGroupName(String customGroupName) { this.customGroupName = customGroupName; }

    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }

    /**
     * Helper method to set tag names from Tag entities.
     * Converts Set<Tag> to List<String> of tag names.
     */
    public void setTagsFromEntities(Set<Tag> tags) {
        this.tagNames = tags.stream()
            .map(Tag::getTagName)
            .collect(Collectors.toList());
    }

    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public Integer getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(Integer currentUserId) { this.currentUserId = currentUserId; }

    /**
     * Whether the document's uploader has a department assigned.
     * When false the "Department" viewer-group option should be hidden in the edit UI.
     */
    public boolean isUploaderHasDepartment() { return uploaderHasDepartment; }
    public void setUploaderHasDepartment(boolean uploaderHasDepartment) {
        this.uploaderHasDepartment = uploaderHasDepartment;
    }

    public String getDtsNumber() { return dtsNumber; }
    public void setDtsNumber(String dtsNumber) { this.dtsNumber = dtsNumber; }

    public Integer getTargetAreaId() { return targetAreaId; }
    public void setTargetAreaId(Integer targetAreaId) { this.targetAreaId = targetAreaId; }

    public String getTargetAreaName() { return targetAreaName; }
    public void setTargetAreaName(String targetAreaName) { this.targetAreaName = targetAreaName; }

    public Integer getTargetOfficeId() { return targetOfficeId; }
    public void setTargetOfficeId(Integer targetOfficeId) { this.targetOfficeId = targetOfficeId; }

    public String getTargetOfficeName() { return targetOfficeName; }
    public void setTargetOfficeName(String targetOfficeName) { this.targetOfficeName = targetOfficeName; }

    public Integer getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Integer targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }

    public String getTargetDepartmentName() { return targetDepartmentName; }
    public void setTargetDepartmentName(String targetDepartmentName) { this.targetDepartmentName = targetDepartmentName; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    /**
     * Helper method to get tags as comma-separated string.
     * Useful for populating the edit input field.
     */
    public String getTagsAsString() {
        if (tagNames == null || tagNames.isEmpty()) return "";
        return String.join(", ", tagNames);
    }
}