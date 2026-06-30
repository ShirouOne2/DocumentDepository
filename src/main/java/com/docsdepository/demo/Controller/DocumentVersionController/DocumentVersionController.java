package com.docsdepository.demo.Controller.DocumentVersionController;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;
import com.docsdepository.demo.Services.PermissionService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Document Version Management
 * Handles uploading new versions, viewing version history, and restoring versions
 */
@Controller
@RequestMapping("/documents")
public class DocumentVersionController {

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PermissionService permissionService;

    /**
     * Helper method to get authenticated user
     */
    private Users getAuthenticatedUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;
        return usersRepository.findByIdWithOfficeHierarchy(userId);
    }

    private boolean isAdmin(Users user) {
        return permissionService.isAdmin(user);
    }

    private boolean canUserAccessDocument(Users user, ImportableInformation doc) {
        return permissionService.canUserAccessDocument(user, doc);
    }

    /**
     * Upload a new version of an existing document
     * 
     * @param id Document ID to create new version for
     * @param file New file to upload
     * @param versionNotes Optional notes about what changed
     * @param session HTTP session for authentication
     * @return Response with success/error status
     */
    
    @Transactional
    @PostMapping("/{id}/upload-new-version")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadNewVersion(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String versionNotes,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                response.put("status", "error");
                response.put("message", "Not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 2. Get the current document
            ImportableInformation currentDoc = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));


            // 2.1 Prevent modifying archived documents
            if (Boolean.TRUE.equals(currentDoc.getIsArchived())) {
                response.put("status", "error");
                response.put("message", "Archived documents are immutable");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }        

            // 3. Verify user owns the document or is admin
            boolean isOwner = currentDoc.getUploadedBy().getUserId().equals(currentUser.getUserId());
            boolean isAdminUser = isAdmin(currentUser);

            if (!isOwner && !isAdminUser) {
                response.put("status", "error");
                response.put("message", "You can only upload new versions of your own documents");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 4. Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No file selected");
                return ResponseEntity.badRequest().body(response);
            }

            // 5. Validate file type
            String contentType = file.getContentType();
            List<String> allowedTypes = List.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain",
                    "image/jpeg",
                    "image/png"
            );

            if (contentType == null || !allowedTypes.contains(contentType)) {
                response.put("status", "error");
                response.put("message", "Unsupported file type");
                return ResponseEntity.badRequest().body(response);
            }

            // 6. Validate file size (10MB limit)
            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                response.put("status", "error");
                response.put("message", "File size exceeds 100MB limit");
                return ResponseEntity.badRequest().body(response);
            }

            // 7. Save the new file
            String newFilename = fileStorageService.saveFile(file);

            // 8. Archive the current document
            currentDoc.setIsArchived(true);
            currentDoc.setArchivedDate(LocalDateTime.now());
            currentDoc.setArchivedBy(currentUser);

            // 9. Create new version (copy metadata from current)
            ImportableInformation newVersion =
            currentDoc.cloneAsNewVersion(currentUser, newFilename, file, versionNotes);


            
            // Copy all metadata (keeps consistency across versions)
            newVersion.setTitle(currentDoc.getTitle());
            newVersion.setDescription(currentDoc.getDescription());
            newVersion.setDocumentClassification(currentDoc.getDocumentClassification());
            newVersion.setIntendedViewerGroup(currentDoc.getIntendedViewerGroup());
            newVersion.setCustomViewerGroup(currentDoc.getCustomViewerGroup());
            newVersion.setTargetArea(currentDoc.getTargetArea());
            newVersion.setTargetOffice(currentDoc.getTargetOffice());
            newVersion.setTargetDepartment(currentDoc.getTargetDepartment());
            newVersion.setTags(
                currentDoc.getTags() != null
                    ? new HashSet<>(currentDoc.getTags())
                    : new HashSet<>()
            );
            
            // Set new file info
            newVersion.setFilename(newFilename);
            newVersion.setOriginalFilename(file.getOriginalFilename());
            newVersion.setFileSizeBytes(file.getSize());
            
            // Set upload info
            newVersion.setUploadedBy(currentUser);
            newVersion.setUploadDate(LocalDateTime.now());
            
            // Set version info
            newVersion.setVersionNumber(currentDoc.getVersionNumber() + 1);
            newVersion.setReplacesDocument(currentDoc);  // Link to old version
            newVersion.setVersionNotes(versionNotes);
            newVersion.setIsArchived(false);

            // 10. Update old document to point to new version
            currentDoc.setReplacedByDocument(newVersion);

            // 11. Save both documents
            documentRepository.save(newVersion);
            documentRepository.save(currentDoc);

            response.put("status", "success");
            response.put("message", "New version uploaded successfully!");
            response.put("newDocumentId", newVersion.getDocumentId());
            response.put("versionNumber", newVersion.getVersionNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to upload new version: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get version history for a document
     * Returns all versions (current and archived) in descending order
     * 
     * @param id Document ID
     * @param session HTTP session for authentication
     * @return List of version information
     */
    @GetMapping("/{id}/versions")
    @ResponseBody
    public ResponseEntity<?> getVersionHistory(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // 2. Get the document
            ImportableInformation document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // 3. Build complete version chain
            List<ImportableInformation> allVersions = new ArrayList<>();
            
            // Go backwards to find all previous versions
            ImportableInformation current = document;
            allVersions.add(current);
            
            while (current.getReplacesDocument() != null) {
                current = current.getReplacesDocument();
                allVersions.add(current);
            }

            // Go forward to find newer versions (if viewing an old version)
            current = document;
            while (current.getReplacedByDocument() != null) {
                current = current.getReplacedByDocument();
                if (!allVersions.contains(current)) {
                    allVersions.add(0, current); // Add at beginning (newer)
                }
            }

            // 4. Sort by version number (descending - newest first)
            allVersions.sort((a, b) -> b.getVersionNumber().compareTo(a.getVersionNumber()));

            // 5. Convert to DTOs
            List<Map<String, Object>> versionDTOs = allVersions.stream()
                    .map(v -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("documentId", v.getDocumentId());
                        dto.put("versionNumber", v.getVersionNumber());
                        dto.put("filename", v.getFilename());
                        dto.put("originalFilename", v.getOriginalFilename());
                        dto.put("fileSizeBytes", v.getFileSizeBytes());
                        dto.put("uploadDate", v.getUploadDate());
                        dto.put("uploadedBy", v.getUploadedBy().getUsername());
                        dto.put("versionNotes", v.getVersionNotes());
                        dto.put("isArchived", v.getIsArchived() != null && v.getIsArchived());
                        dto.put("isCurrent", v.getIsArchived() == null || !v.getIsArchived());
                        
                        // Add archived info if applicable
                        if (v.getIsArchived() != null && v.getIsArchived()) {
                            dto.put("archivedDate", v.getArchivedDate());
                            if (v.getArchivedBy() != null) {
                                dto.put("archivedBy", v.getArchivedBy().getUsername());
                            }
                        }
                        
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(versionDTOs);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error loading version history: " + e.getMessage()));
        }
    }

    /**
     * Get the current (latest) version of a document
     * 
     * @param id Document ID (can be any version)
     * @param session HTTP session for authentication
     * @return Current version information
     */
    @GetMapping("/{id}/current-version")
    @ResponseBody
    public ResponseEntity<?> getCurrentVersion(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // 2. Get the document
            ImportableInformation document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // 3. Find the current (non-archived) version
            ImportableInformation current = document;
            while (current.getReplacedByDocument() != null) {
                current = current.getReplacedByDocument();
            }

            // 4. Build response
            Map<String, Object> response = new HashMap<>();
            response.put("documentId", current.getDocumentId());
            response.put("versionNumber", current.getVersionNumber());
            response.put("title", current.getTitle());
            response.put("filename", current.getFilename());
            response.put("originalFilename", current.getOriginalFilename());
            response.put("uploadDate", current.getUploadDate());
            response.put("isCurrent", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error finding current version: " + e.getMessage()));
        }
    }

    /**
     * Check if a document has multiple versions
     * 
     * @param id Document ID
     * @param session HTTP session for authentication
     * @return Boolean indicating if multiple versions exist
     */
    @GetMapping("/{id}/has-versions")
    @ResponseBody
    public ResponseEntity<?> hasMultipleVersions(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // 2. Get the document
            ImportableInformation document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // 3. Check if there are other versions
            boolean hasOtherVersions = document.getReplacesDocument() != null || 
                                      document.getReplacedByDocument() != null;

            // 4. Count total versions
            int versionCount = 1;
            ImportableInformation current = document;
            
            // Count backwards
            while (current.getReplacesDocument() != null) {
                current = current.getReplacesDocument();
                versionCount++;
            }
            
            // Count forwards
            current = document;
            while (current.getReplacedByDocument() != null) {
                current = current.getReplacedByDocument();
                versionCount++;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("hasMultipleVersions", hasOtherVersions);
            response.put("versionCount", versionCount);
            response.put("currentVersionNumber", document.getVersionNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Compare two versions of a document
     * Returns metadata comparison (not file content comparison)
     * 
     * @param id1 First version ID
     * @param id2 Second version ID
     * @param session HTTP session for authentication
     * @return Comparison data
     */
    @GetMapping("/compare-versions")
    @ResponseBody
    public ResponseEntity<?> compareVersions(
            @RequestParam Integer id1,
            @RequestParam Integer id2,
            HttpSession session) {

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // 2. Get both documents
            ImportableInformation version1 = documentRepository.findById(id1)
                    .orElseThrow(() -> new RuntimeException("Version 1 not found"));
            ImportableInformation version2 = documentRepository.findById(id2)
                    .orElseThrow(() -> new RuntimeException("Version 2 not found"));

            // 3. Build comparison
            Map<String, Object> comparison = new HashMap<>();
            
            Map<String, Object> v1Info = new HashMap<>();
            v1Info.put("documentId", version1.getDocumentId());
            v1Info.put("versionNumber", version1.getVersionNumber());
            v1Info.put("filename", version1.getOriginalFilename());
            v1Info.put("fileSize", version1.getFileSizeBytes());
            v1Info.put("uploadDate", version1.getUploadDate());
            v1Info.put("uploadedBy", version1.getUploadedBy().getUsername());
            v1Info.put("versionNotes", version1.getVersionNotes());
            
            Map<String, Object> v2Info = new HashMap<>();
            v2Info.put("documentId", version2.getDocumentId());
            v2Info.put("versionNumber", version2.getVersionNumber());
            v2Info.put("filename", version2.getOriginalFilename());
            v2Info.put("fileSize", version2.getFileSizeBytes());
            v2Info.put("uploadDate", version2.getUploadDate());
            v2Info.put("uploadedBy", version2.getUploadedBy().getUsername());
            v2Info.put("versionNotes", version2.getVersionNotes());
            
            comparison.put("version1", v1Info);
            comparison.put("version2", v2Info);
            
            // Add differences
            Map<String, Object> differences = new HashMap<>();
            differences.put("fileSizeDifference", version2.getFileSizeBytes() - version1.getFileSizeBytes());
            differences.put("versionGap", version2.getVersionNumber() - version1.getVersionNumber());
            differences.put("sameTitle", version1.getTitle().equals(version2.getTitle()));
            
            comparison.put("differences", differences);

            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error comparing versions: " + e.getMessage()));
        }
    }

    /**
     * Get version statistics for a document
     * 
     * @param id Document ID
     * @param session HTTP session for authentication
     * @return Version statistics
     */
    @GetMapping("/{id}/version-stats")
    @ResponseBody
    public ResponseEntity<?> getVersionStats(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            // 1. Authentication check
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }

            // 2. Get the document
            ImportableInformation document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // 3. Build version chain
            List<ImportableInformation> allVersions = new ArrayList<>();
            ImportableInformation current = document;
            
            // Go to oldest version
            while (current.getReplacesDocument() != null) {
                current = current.getReplacesDocument();
            }
            
            // Collect all versions forward
            allVersions.add(current);
            while (current.getReplacedByDocument() != null) {
                current = current.getReplacedByDocument();
                allVersions.add(current);
            }

            // 4. Calculate statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalVersions", allVersions.size());
            stats.put("oldestVersion", allVersions.get(0).getVersionNumber());
            stats.put("newestVersion", allVersions.get(allVersions.size() - 1).getVersionNumber());
            stats.put("firstUploadDate", allVersions.get(0).getUploadDate());
            stats.put("lastUploadDate", allVersions.get(allVersions.size() - 1).getUploadDate());
            
            // Calculate total file size across all versions
            long totalSize = allVersions.stream()
                    .mapToLong(v -> v.getFileSizeBytes() != null ? v.getFileSizeBytes() : 0)
                    .sum();
            stats.put("totalStorageUsed", totalSize);
            
            // Count unique uploaders
            long uniqueUploaders = allVersions.stream()
                    .map(v -> v.getUploadedBy().getUserId())
                    .distinct()
                    .count();
            stats.put("uniqueUploaders", uniqueUploaders);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error calculating version stats: " + e.getMessage()));
        }
    }
}