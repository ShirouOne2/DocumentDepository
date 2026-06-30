package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FileIntegrityService {

    private static final Logger log = Logger.getLogger(FileIntegrityService.class.getName());

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirectory;

    /**
     * Check if a single document's file exists
     */
    public boolean fileExists(ImportableInformation document) {
        if (document == null || document.getFilename() == null) {
            return false;
        }

        File file = new File(uploadDirectory + File.separator + document.getFilename());
        return file.exists() && file.isFile();
    }

    /**
     * Get file status for a single document
     */
    public FileStatus getFileStatus(ImportableInformation document) {
        boolean exists = fileExists(document);
        File file = new File(uploadDirectory + File.separator + document.getFilename());
        
        return new FileStatus(
            document.getDocumentId(),
            document.getFilename(),
            exists,
            exists ? file.length() : 0,
            document.getUploadedBy() != null ? document.getUploadedBy().getUsername() : "Unknown"
        );
    }

    /**
     * Scan all documents and return broken file links (PAGINATED)
     */
    public Page<FileStatus> findBrokenFiles(Pageable pageable) {
        List<FileStatus> allBrokenFiles = findBrokenFiles();
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allBrokenFiles.size());
        
        List<FileStatus> pageContent = allBrokenFiles.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allBrokenFiles.size());
    }

    /**
     * Scan all documents and return broken file links
     */
    public List<FileStatus> findBrokenFiles() {
        List<FileStatus> brokenFiles = new ArrayList<>();
        List<ImportableInformation> allDocuments = documentRepository.findByIsArchivedFalse();

        log.log(Level.INFO, "Starting file integrity scan for " + allDocuments.size() + " documents...");

        for (ImportableInformation doc : allDocuments) {
            if (!fileExists(doc)) {
                brokenFiles.add(getFileStatus(doc));
            }
        }

        log.log(Level.WARNING, "Found " + brokenFiles.size() + " broken file links");
        return brokenFiles;
    }

    /**
     * ✅ NEW: Search broken files by filename, document ID, or uploader
     */
    public Page<FileStatus> searchBrokenFiles(String query, Pageable pageable) {
        List<FileStatus> allBrokenFiles = findBrokenFiles();
        
        // Filter by search query
        String lowerQuery = query.toLowerCase().trim();
        List<FileStatus> filtered = allBrokenFiles.stream()
            .filter(file -> {
                // Search by filename
                if (file.getFilename() != null && 
                    file.getFilename().toLowerCase().contains(lowerQuery)) {
                    return true;
                }
                
                // Search by document ID
                if (file.getDocumentId().toString().contains(lowerQuery)) {
                    return true;
                }
                
                // Search by uploader
                if (file.getUploadedBy() != null && 
                    file.getUploadedBy().toLowerCase().contains(lowerQuery)) {
                    return true;
                }
                
                return false;
            })
            .collect(Collectors.toList());
        
        log.log(Level.INFO, "Search '" + query + "' found " + filtered.size() + " results");
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        List<FileStatus> pageContent = start < filtered.size() ? 
                                        filtered.subList(start, end) : 
                                        new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * ✅ NEW: Archive a single broken file
     */
    @Transactional
    public boolean archiveBrokenFile(Integer documentId) {
        try {
            ImportableInformation document = documentRepository.findById(documentId).orElse(null);
            
            if (document != null) {
                document.setIsArchived(true);
                documentRepository.save(document);
                
                log.log(Level.INFO, "Archived broken file: Document ID " + documentId + 
                       " (" + document.getFilename() + ")");
                return true;
            } else {
                log.log(Level.WARNING, "Document not found for archiving: ID " + documentId);
                return false;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error archiving document ID " + documentId, e);
            return false;
        }
    }

    /**
     * ✅ NEW: Archive all broken files
     */
    @Transactional
    public int archiveAllBrokenFiles() {
        List<FileStatus> brokenFiles = findBrokenFiles();
        int archivedCount = 0;
        
        log.log(Level.INFO, "Starting bulk archive of " + brokenFiles.size() + " broken files...");
        
        for (FileStatus file : brokenFiles) {
            if (archiveBrokenFile(file.getDocumentId())) {
                archivedCount++;
            }
        }
        
        log.log(Level.INFO, "Bulk archive completed: " + archivedCount + 
               " of " + brokenFiles.size() + " files archived");
        
        return archivedCount;
    }

    /**
     * Get integrity report statistics
     */
    public IntegrityReport getIntegrityReport() {
        List<ImportableInformation> allDocuments = documentRepository.findByIsArchivedFalse();
        int totalDocuments = allDocuments.size();
        int brokenFiles = 0;
        int validFiles = 0;
        long totalSize = 0;

        for (ImportableInformation doc : allDocuments) {
            if (fileExists(doc)) {
                validFiles++;
                File file = new File(uploadDirectory + File.separator + doc.getFilename());
                totalSize += file.length();
            } else {
                brokenFiles++;
            }
        }

        return new IntegrityReport(totalDocuments, validFiles, brokenFiles, totalSize);
    }

    /**
     * Check files for a specific user
     */
    public List<FileStatus> checkUserFiles(Integer userId) {
        List<FileStatus> results = new ArrayList<>();
        List<ImportableInformation> userDocuments = documentRepository.findByUploadedBy_UserId(userId);

        for (ImportableInformation doc : userDocuments) {
            results.add(getFileStatus(doc));
        }

        return results;
    }

    // Inner Classes for DTOs
    public static class FileStatus {
        private Integer documentId;
        private String filename;
        private boolean exists;
        private long fileSize;
        private String uploadedBy;

        public FileStatus(Integer documentId, String filename, boolean exists, long fileSize, String uploadedBy) {
            this.documentId = documentId;
            this.filename = filename;
            this.exists = exists;
            this.fileSize = fileSize;
            this.uploadedBy = uploadedBy;
        }

        // Getters
        public Integer getDocumentId() { return documentId; }
        public String getFilename() { return filename; }
        public boolean isExists() { return exists; }
        public long getFileSize() { return fileSize; }
        public String getUploadedBy() { return uploadedBy; }
    }

    public static class IntegrityReport {
        private int totalDocuments;
        private int validFiles;
        private int brokenFiles;
        private long totalSizeBytes;

        public IntegrityReport(int totalDocuments, int validFiles, int brokenFiles, long totalSizeBytes) {
            this.totalDocuments = totalDocuments;
            this.validFiles = validFiles;
            this.brokenFiles = brokenFiles;
            this.totalSizeBytes = totalSizeBytes;
        }

        // Getters
        public int getTotalDocuments() { return totalDocuments; }
        public int getValidFiles() { return validFiles; }
        public int getBrokenFiles() { return brokenFiles; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        
        public double getIntegrityPercentage() {
            return totalDocuments > 0 ? (validFiles * 100.0 / totalDocuments) : 0;
        }
        
        public String getTotalSizeFormatted() {
            if (totalSizeBytes < 1024) return totalSizeBytes + " B";
            if (totalSizeBytes < 1024 * 1024) return String.format("%.2f KB", totalSizeBytes / 1024.0);
            if (totalSizeBytes < 1024 * 1024 * 1024) return String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024));
            return String.format("%.2f GB", totalSizeBytes / (1024.0 * 1024 * 1024));
        }
    }
}