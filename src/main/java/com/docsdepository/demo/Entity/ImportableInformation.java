package com.docsdepository.demo.Entity; 

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "importable_information")
public class ImportableInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_classification_id", nullable = false)
    private DocumentClassification documentClassification;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false)
    private String filename;

    @Column(name = "date_created", updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    // ✅ NEW FIELDS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Users uploadedBy;

    @Column(name = "intended_viewer_group")
    private String intendedViewerGroup;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Users getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Users uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getIntendedViewerGroup() {
        return intendedViewerGroup;
    }

    public void setIntendedViewerGroup(String intendedViewerGroup) {
        this.intendedViewerGroup = intendedViewerGroup;
    }
}