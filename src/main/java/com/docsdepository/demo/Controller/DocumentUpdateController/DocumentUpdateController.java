package com.docsdepository.demo.Controller.DocumentUpdateController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.IntendedViewerGroup;
import com.docsdepository.demo.Entity.Tag;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Entity.Area;
import com.docsdepository.demo.Entity.Office;
import com.docsdepository.demo.Entity.Department;
import com.docsdepository.demo.Repository.CustomViewerGroupRepository;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.IntendedViewerGroupRepository;
import com.docsdepository.demo.Repository.TagRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Repository.AreaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Services.NotificationService;
import com.docsdepository.demo.Services.PermissionService;

import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

/**
 * Controller for updating document metadata (classification, tags, viewer groups, description)
 * Handles both regular users (owner only) and admin users (all documents)
 */
@Controller
@RequestMapping("/documents")
public class DocumentUpdateController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUpdateController.class);

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private DocumentClassificationRepository classificationRepository;

    @Autowired
    private IntendedViewerGroupRepository viewerGroupRepository;

    @Autowired
    private CustomViewerGroupRepository customViewerGroupRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PermissionService permissionService;

    private Users getAuthenticatedUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;
        return usersRepository.findByIdWithOfficeHierarchy(userId);
    }

    private boolean isAdmin(Users user) {
        return permissionService.isAdmin(user);
    }

    /**
     * Update document metadata (classification, tags, viewer groups, description)
     * 
     * PERMISSIONS:
     * - Regular users: Can only edit their own documents
     * - Admin users: Can edit any document
     * 
     * @param documentId Document ID to update
     * @param classificationId New classification ID
     * @param tags Comma-separated tag names
     * @param viewerGroupId Viewer group ID (1=All, 2=Area, 3=Office, 4=Department, 5=Custom)
     * @param customGroupId Custom viewer group ID (required if viewerGroupId=5)
     * @param description New description
     * @param session HTTP session for authentication
     * @return Response with success/error status
     */
    @Transactional
    @PostMapping("/{documentId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable Integer documentId,
            @RequestParam Integer classificationId,
            @RequestParam(required = false) String dtsNumber,
            @RequestParam String tags,
            @RequestParam Integer viewerGroupId,
            @RequestParam(required = false) Integer customGroupId,
            @RequestParam String description,
            @RequestParam(required = false) Integer targetOfficeId,
            @RequestParam(required = false) Integer targetDepartmentId,
            @RequestParam(required = false) String contractStartDate,
            @RequestParam(required = false) String contractEndDate,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Get current user
            Users currentUser = getAuthenticatedUser(session);
            if (currentUser == null) {
                response.put("status", "error");
                response.put("message", "Not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 2. Get the document
            ImportableInformation document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            // 3. Check permissions
            boolean isOwner = document.getUploadedBy().getUserId().equals(currentUser.getUserId());
            boolean isAdminUser = isAdmin(currentUser);

            if (!isOwner && !isAdminUser) {
                response.put("status", "error");
                response.put("message", "You don't have permission to edit this document");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 4. Update classification
            DocumentClassification classification = classificationRepository.findById(classificationId)
                    .orElseThrow(() -> new RuntimeException("Classification not found"));
            document.setDocumentClassification(classification);

            // 5. Update viewer group
            IntendedViewerGroup viewerGroup = viewerGroupRepository.findById(viewerGroupId)
                    .orElseThrow(() -> new RuntimeException("Viewer group not found"));
            document.setIntendedViewerGroup(viewerGroup);

            // 6. Update custom viewer group if applicable
            if (viewerGroupId == 5) {
                if (customGroupId == null) {
                    response.put("status", "error");
                    response.put("message", "Custom group is required when viewer group is 'Custom Group'");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                
                CustomViewerGroup customGroup = customViewerGroupRepository.findById(customGroupId)
                        .orElseThrow(() -> new RuntimeException("Custom viewer group not found"));
                document.setCustomViewerGroup(customGroup);
            } else {
                document.setCustomViewerGroup(null);
            }

            // 7. Update description
            document.setDescription(description != null && !description.trim().isEmpty() 
                ? description.trim() 
                : null);

            // 8. Update DTS number
            document.setDtsNumber(dtsNumber != null && !dtsNumber.trim().isEmpty()
                ? dtsNumber.trim()
                : null);

            // 8b. Update contract dates (null-safe, clears if blank)
            document.setContractStartDate(
                contractStartDate != null && !contractStartDate.isBlank()
                    ? LocalDate.parse(contractStartDate) : null);
            document.setContractEndDate(
                contractEndDate != null && !contractEndDate.isBlank()
                    ? LocalDate.parse(contractEndDate) : null);

            // 9. Update target org fields (admin only — regular users keep their original snapshot)
            if (isAdminUser) {
                if (targetOfficeId != null) {
                    officeRepository.findById(targetOfficeId).ifPresent(office -> {
                        document.setTargetOffice(office);
                        document.setTargetArea(office.getArea());
                    });
                }
                if (targetDepartmentId != null) {
                    departmentRepository.findById(targetDepartmentId).ifPresent(document::setTargetDepartment);
                } else if (targetOfficeId != null) {
                    // Admin changed office but cleared department — clear it
                    document.setTargetDepartment(null);
                }
            }

            // 10. Update tags
            Set<Tag> newTags = new HashSet<>();
            if (tags != null && !tags.trim().isEmpty()) {
                List<String> tagNames = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());

                for (String tagName : tagNames) {
                    Tag tag = tagRepository.findByTagName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setTagName(tagName);
                                return tagRepository.save(newTag);
                            });
                    newTags.add(tag);
                }
            }
            document.setTags(newTags);

            // 11. Save the updated document
            documentRepository.save(document);

            // 12. Re-notify if viewer group or target changed so the new audience is informed
            try {
                notificationService.notifyDocumentShared(document);
            } catch (Exception notifEx) {
                // Notification failure must never roll back the update
                logger.warn("Notification failed after document update for id {}: {}",
                        documentId, notifEx.getMessage());
            }

            response.put("status", "success");
            response.put("message", "Document updated successfully");
            response.put("documentId", documentId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to update document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}