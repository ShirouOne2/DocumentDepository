package com.docsdepository.demo.Controller.DocumentViewController;

import com.docsdepository.demo.Controller.DTO.CommentDTO;
import com.docsdepository.demo.Controller.DTO.DocumentDetailsDTO;
import com.docsdepository.demo.Entity.DocumentComment;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.DocumentCommentRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.NotificationService;
import com.docsdepository.demo.Services.PermissionService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/documents")
public class DocumentViewController {

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private DocumentCommentRepository commentRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PermissionService permissionService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ─────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────
    // GET /documents/details/{id}
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/details/{id}")
    @ResponseBody
    public ResponseEntity<?> getDocumentDetails(@PathVariable Integer id, HttpSession session) {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findByIdWithDetails(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canUserAccessDocument(currentUser, doc)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Build comments
        List<CommentDTO> commentDTOs = commentRepository
                .findByDocumentDocumentIdOrderByCreatedAtDesc(id)
                .stream()
                .filter(DocumentComment::getIsActive)
                .map(c -> new CommentDTO(
                        c.getCommentId(),
                        c.getCommentText(),
                        c.getUser().getUserId(),
                        c.getUser().getUsername(),
                        c.getCreatedAt()
                ))
                .toList();

        // Build DTO
        DocumentDetailsDTO dto = new DocumentDetailsDTO();

        // Basic info
        dto.setDocumentId(doc.getDocumentId());
        dto.setTitle(doc.getTitle());
        dto.setDescription(doc.getDescription());
        dto.setFilename(doc.getFilename());
        dto.setUploadedBy(doc.getUploadedBy().getUsername());
        dto.setUploadedById(doc.getUploadedBy().getUserId());   // ← NEW: numeric owner ID
        dto.setDateCreated(doc.getCreatedAt());
        dto.setUploadDate(doc.getUploadDate());
        dto.setCurrentUserId(currentUser.getUserId());

        // Classification
        dto.setDocumentClassification(doc.getDocumentClassification().getName());
        dto.setClassificationId(doc.getDocumentClassification().getId());

        // Viewer group
        dto.setIntendedViewerGroup(doc.getIntendedViewerGroup().getName());
        dto.setViewerGroupId(doc.getIntendedViewerGroup().getId());

        // Custom group
        if (doc.getCustomViewerGroup() != null) {
            dto.setCustomGroupId(doc.getCustomViewerGroup().getGroupId());
            dto.setCustomGroupName(doc.getCustomViewerGroup().getGroupName());
        }

        // Tags
        dto.setTagsFromEntities(doc.getTags());

        // ← NEW: does the uploader have a department?
        //   When false the "Department" option must be hidden in the edit form.
        dto.setUploaderHasDepartment(doc.getUploadedBy().getDepartment() != null);

        // DTS Number
        dto.setDtsNumber(doc.getDtsNumber());

        // Contract dates
        dto.setContractStartDate(doc.getContractStartDate());
        dto.setContractEndDate(doc.getContractEndDate());

        // Target org fields (for admin edit modal dropdowns)
        if (doc.getTargetArea() != null) {
            dto.setTargetAreaId(doc.getTargetArea().getId());
            dto.setTargetAreaName(doc.getTargetArea().getName());
        }
        if (doc.getTargetOffice() != null) {
            dto.setTargetOfficeId(doc.getTargetOffice().getOfficeId());
            dto.setTargetOfficeName(doc.getTargetOffice().getOfficeName());
        }
        if (doc.getTargetDepartment() != null) {
            dto.setTargetDepartmentId(doc.getTargetDepartment().getId());
            dto.setTargetDepartmentName(doc.getTargetDepartment().getName());
        }

        // Comments
        dto.setComments(commentDTOs);
        dto.setCommentCount(commentDTOs.size());

        return ResponseEntity.ok(dto);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /documents/view/{id}
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Integer id, HttpSession session) throws IOException {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findByIdWithDetails(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canUserAccessDocument(currentUser, doc)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path path = Paths.get(uploadDir, doc.getFilename());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + URLEncoder.encode(doc.getFilename(), "UTF-8") + "\"");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /documents/download/{id}
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Integer id, HttpSession session) throws IOException {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findByIdWithDetails(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canUserAccessDocument(currentUser, doc)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path path = Paths.get(uploadDir, doc.getFilename());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(doc.getFilename())
                                .build()
                                .toString())
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /documents/{id}/comments
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/comments")
    @ResponseBody
    public ResponseEntity<?> addComment(
            @PathVariable Integer id,
            @RequestParam String commentText,
            HttpSession session) {

        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findByIdWithDetails(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canUserAccessDocument(currentUser, doc)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DocumentComment comment = new DocumentComment();
        comment.setDocument(doc);
        comment.setUser(currentUser);
        comment.setCommentText(commentText);
        DocumentComment savedComment = commentRepository.save(comment);

        notificationService.notifyNewComment(savedComment);

        CommentDTO responseDTO = new CommentDTO(
            savedComment.getCommentId(),
            savedComment.getCommentText(),
            currentUser.getUserId(),
            currentUser.getUsername(),
            savedComment.getCreatedAt()
        );

        return ResponseEntity.ok(responseDTO);
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /documents/archive/{id}
    // Owner OR admin can archive.
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/archive/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveDocument(
            @PathVariable Integer id, HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Session expired. Please log in again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            ImportableInformation document = documentRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

            boolean isOwner = document.getUploadedBy().getUserId().equals(currentUser.getUserId());

            // Allow owner OR admin to archive
            if (!isOwner && !isAdmin(currentUser)) {
                response.put("success", false);
                response.put("message", "You can only archive your own documents.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            document.setIsArchived(true);
            document.setArchivedDate(LocalDateTime.now());
            document.setArchivedBy(currentUser);
            documentRepository.save(document);

            response.put("success", true);
            response.put("message", "Document archived successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error archiving document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE /documents/comments/{commentId}
    // ─────────────────────────────────────────────────────────────────────

    @DeleteMapping("/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveComment(
            @PathVariable Integer commentId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Session expired. Please log in again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            DocumentComment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

            if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
                response.put("success", false);
                response.put("message", "You can only delete your own comments.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            comment.setIsActive(false);
            commentRepository.save(comment);

            response.put("success", true);
            response.put("message", "Comment deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting comment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /documents/{id}/versions
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/documents/{id}/versions")
    @ResponseBody
    public ResponseEntity<?> getVersionHistory(@PathVariable Integer id, HttpSession session) {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

            List<ImportableInformation> versions = documentRepository.findVersionHistory(id);

            List<Map<String, Object>> versionDTOs = versions.stream()
                .map(v -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("documentId",       v.getDocumentId());
                    dto.put("versionNumber",    v.getVersionNumber());
                    dto.put("originalFilename", v.getOriginalFilename());
                    dto.put("fileSizeBytes",    v.getFileSizeBytes());
                    dto.put("uploadDate",       v.getUploadDate());
                    dto.put("uploadedBy",       v.getUploadedBy().getUsername());
                    dto.put("versionNotes",     v.getVersionNotes());
                    return dto;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(versionDTOs);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}