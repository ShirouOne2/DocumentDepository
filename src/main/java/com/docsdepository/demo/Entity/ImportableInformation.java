package com.docsdepository.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.Area;
import com.docsdepository.demo.Entity.Office;
import com.docsdepository.demo.Entity.Department;

@Entity
@Table(name = "documents")
public class ImportableInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    private DocumentClassification documentClassification;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "document_date")
    private LocalDateTime documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private Users uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_group_id", nullable = false)
    private IntendedViewerGroup intendedViewerGroup;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_date")
    private LocalDateTime archivedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by_user_id")
    private Users archivedBy;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_viewer_group_id")
    private CustomViewerGroup customViewerGroup;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_document_id")
    private ImportableInformation replacesDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_document_id")
    private ImportableInformation replacedByDocument;

    @Column(name = "version_notes", length = 500)
    private String versionNotes;

    @Column(name = "dts_number")
    private String dtsNumber;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    // ── Contract expiry alert flags ──────────────────────────────────────────
    // Flipped to true once each alert fires. Non-contract docs leave these false
    // permanently — the scheduler only touches rows where contractEndDate IS NOT NULL.
    @Column(name = "expiry_alert_30_sent", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean expiryAlert30Sent = false;

    @Column(name = "expiry_alert_7_sent", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean expiryAlert7Sent = false;

    // ── Visibility target fields ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_area_id")
    private Area targetArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_office_id")
    private Office targetOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "document_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public Integer getDocumentId() { return documentId; }
    public void setDocumentId(Integer documentId) { this.documentId = documentId; }

    public DocumentClassification getDocumentClassification() { return documentClassification; }
    public void setDocumentClassification(DocumentClassification dc) { this.documentClassification = dc; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public LocalDateTime getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDateTime documentDate) { this.documentDate = documentDate; }

    public Users getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Users uploadedBy) { this.uploadedBy = uploadedBy; }

    public IntendedViewerGroup getIntendedViewerGroup() { return intendedViewerGroup; }
    public void setIntendedViewerGroup(IntendedViewerGroup ivg) { this.intendedViewerGroup = ivg; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public LocalDateTime getArchivedDate() { return archivedDate; }
    public void setArchivedDate(LocalDateTime archivedDate) { this.archivedDate = archivedDate; }

    public Users getArchivedBy() { return archivedBy; }
    public void setArchivedBy(Users archivedBy) { this.archivedBy = archivedBy; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }

    public void addTag(Tag tag) { this.tags.add(tag); tag.getDocuments().add(this); }
    public void removeTag(Tag tag) { this.tags.remove(tag); tag.getDocuments().remove(this); }

    public CustomViewerGroup getCustomViewerGroup() { return customViewerGroup; }
    public void setCustomViewerGroup(CustomViewerGroup cvg) { this.customViewerGroup = cvg; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public ImportableInformation getReplacesDocument() { return replacesDocument; }
    public void setReplacesDocument(ImportableInformation r) { this.replacesDocument = r; }

    public ImportableInformation getReplacedByDocument() { return replacedByDocument; }
    public void setReplacedByDocument(ImportableInformation r) { this.replacedByDocument = r; }

    public String getVersionNotes() { return versionNotes; }
    public void setVersionNotes(String versionNotes) { this.versionNotes = versionNotes; }

    public String getDtsNumber() { return dtsNumber; }
    public void setDtsNumber(String dtsNumber) { this.dtsNumber = dtsNumber; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public Boolean getExpiryAlert30Sent() { return expiryAlert30Sent; }
    public void setExpiryAlert30Sent(Boolean v) { this.expiryAlert30Sent = v; }

    public Boolean getExpiryAlert7Sent() { return expiryAlert7Sent; }
    public void setExpiryAlert7Sent(Boolean v) { this.expiryAlert7Sent = v; }

    public Area getTargetArea() { return targetArea; }
    public void setTargetArea(Area targetArea) { this.targetArea = targetArea; }

    public Office getTargetOffice() { return targetOffice; }
    public void setTargetOffice(Office targetOffice) { this.targetOffice = targetOffice; }

    public Department getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(Department targetDepartment) { this.targetDepartment = targetDepartment; }

    public ImportableInformation cloneAsNewVersion(
            Users uploadedBy,
            String newFilename,
            MultipartFile file,
            String versionNotes
    ) {
        ImportableInformation v = new ImportableInformation();

        v.setTitle(this.title);
        v.setDescription(this.description);
        v.setDocumentClassification(this.documentClassification);
        v.setIntendedViewerGroup(this.intendedViewerGroup);
        v.setCustomViewerGroup(this.customViewerGroup);
        v.setDtsNumber(this.dtsNumber);
        v.setTargetArea(this.targetArea);
        v.setTargetOffice(this.targetOffice);
        v.setTargetDepartment(this.targetDepartment);
        v.setContractStartDate(this.contractStartDate);
        v.setContractEndDate(this.contractEndDate);

        // New version resets alert flags — treat as a fresh contract
        v.setExpiryAlert30Sent(false);
        v.setExpiryAlert7Sent(false);

        v.setTags(this.tags != null ? new HashSet<>(this.tags) : new HashSet<>());

        v.setFilename(newFilename);
        v.setOriginalFilename(file.getOriginalFilename());
        v.setFileSizeBytes(file.getSize());

        v.setVersionNumber(this.versionNumber + 1);
        v.setReplacesDocument(this);
        v.setVersionNotes(versionNotes);

        v.setUploadedBy(uploadedBy);
        v.setUploadDate(LocalDateTime.now());
        v.setIsArchived(false);

        return v;
    }
}