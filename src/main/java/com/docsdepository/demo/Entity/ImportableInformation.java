package com.docsdepository.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "importables_information")
public class ImportableInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_classification_id", nullable = false)
    private DocumentClassification documentClassification;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "date_created", updatable = false)
    private LocalDateTime dateCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "username")
    private Users uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intended_viewer_group", referencedColumnName = "id")
    private IntendedViewerGroup intendedViewerGroup;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Boolean getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(Boolean isArchived) {
        this.isArchived = isArchived;
    }
    @Column(name = "archived_date")
    private LocalDateTime archivedDate;

    public LocalDateTime getArchivedDate() {
        return archivedDate;
    }

    public void setArchivedDate(LocalDateTime archivedDate) {
        this.archivedDate = archivedDate;
    }

    // Getters and Setters
    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Integer documentId) {
        this.documentId = documentId;
    }

    public DocumentClassification getDocumentClassification() {
        return documentClassification;
    }

    public void setDocumentClassification(DocumentClassification documentClassification) {
        this.documentClassification = documentClassification;
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

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Users getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Users uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public IntendedViewerGroup getIntendedViewerGroup() {
        return intendedViewerGroup;
    }
    
    public void setIntendedViewerGroup(IntendedViewerGroup intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }
}