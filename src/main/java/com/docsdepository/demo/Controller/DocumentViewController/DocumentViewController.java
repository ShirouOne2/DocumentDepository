package com.docsdepository.demo.Controller.DocumentViewController;

import com.docsdepository.demo.Controller.DTO.CommentDTO;
import com.docsdepository.demo.Controller.DTO.DocumentDetailsDTO;
import com.docsdepository.demo.Entity.DocumentComment;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.DocumentCommentRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
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

@Controller
@RequestMapping("/documents")
public class DocumentViewController {

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private DocumentCommentRepository commentRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Users getAuthenticatedUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;
        return usersRepository.findByIdWithOfficeHierarchy(userId);
    }

    @GetMapping("/details/{id}")
    @ResponseBody
    public ResponseEntity<?> getDocumentDetails(@PathVariable Integer id, HttpSession session) {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        List<DocumentComment> comments = commentRepository
                .findByDocumentDocumentIdOrderByCreatedDateDesc(id);

        DocumentDetailsDTO dto = new DocumentDetailsDTO();
        dto.setDocumentId(doc.getDocumentId());
        dto.setTitle(doc.getTitle());
        dto.setDescription(doc.getDescription());
        dto.setFilename(doc.getFilename());
        dto.setUploadedBy(doc.getUploadedBy().getUsername());
        dto.setDateCreated(doc.getDateCreated());
        dto.setDocumentClassification(doc.getDocumentClassification().getName());
        dto.setIntendedViewerGroup(doc.getIntendedViewerGroup().getName());
        dto.setComments(comments);
        dto.setCommentCount(comments.size());
        dto.setUploadDate(doc.getUploadDate());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Integer id, HttpSession session) throws IOException {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(uploadDir, doc.getFilename());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + URLEncoder.encode(doc.getFilename(), "UTF-8") + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(resource);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Integer id, HttpSession session) throws IOException {
        Users currentUser = getAuthenticatedUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImportableInformation doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(uploadDir, doc.getFilename());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(doc.getFilename())
                                .build()
                                .toString())
                .body(resource);
    }

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

        ImportableInformation doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        DocumentComment comment = new DocumentComment();
        comment.setDocument(doc);
        comment.setUser(currentUser);
        comment.setCommentText(commentText);
        DocumentComment savedComment = commentRepository.save(comment);

        CommentDTO responseDTO = new CommentDTO(
            savedComment.getCommentId(),
            savedComment.getCommentText(),
            currentUser.getUsername(),
            savedComment.getCreatedDate()
        );

        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/archive/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveDocument(@PathVariable Integer id, HttpSession session) {
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
            
            if (!document.getUploadedBy().getUserId().equals(currentUser.getUserId())) {
                response.put("success", false);
                response.put("message", "You can only archive your own documents");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            document.setIsArchived(true);
            document.setArchivedDate(LocalDateTime.now());
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
}