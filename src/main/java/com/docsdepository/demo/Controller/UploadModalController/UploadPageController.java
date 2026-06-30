package com.docsdepository.demo.Controller.UploadModalController;

import com.docsdepository.demo.Controller.DTO.UploadForm;
import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.Area;
import com.docsdepository.demo.Entity.Office;
import com.docsdepository.demo.Entity.Department;
import com.docsdepository.demo.Entity.IntendedViewerGroup;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.IntendedViewerGroupRepository;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;
import com.docsdepository.demo.Services.CustomViewerGroupService;
import com.docsdepository.demo.Services.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.docsdepository.demo.Services.TagService;
import com.docsdepository.demo.Entity.Tag;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadPageController {

    private static final Logger logger = LoggerFactory.getLogger(UploadPageController.class);

    private final ImportableInformationRepository importableInformationRepository;
    private final DocumentClassificationRepository documentClassificationRepository;
    private final UsersRepository usersRepository;
    private final FileStorageService fileStorageService;
    
    @Autowired
    private IntendedViewerGroupRepository intendedViewerGroupRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TagService tagService;

    @Autowired
    private CustomViewerGroupService customViewerGroupService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    public UploadPageController(
            ImportableInformationRepository importableInformationRepository,
            DocumentClassificationRepository documentClassificationRepository,
            UsersRepository usersRepository,
            FileStorageService fileStorageService) {

        this.importableInformationRepository = importableInformationRepository;
        this.documentClassificationRepository = documentClassificationRepository;
        this.usersRepository = usersRepository;
        this.fileStorageService = fileStorageService;
    }
    
    @GetMapping("/uploadpage")
    public String getUploadform(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }
        
        List<DocumentClassification> classifications = documentClassificationRepository.findAll();
        
        model.addAttribute("uploadForm", new UploadForm());
        model.addAttribute("classifications", classifications);
        boolean isAdmin = currentUser.getRole() != null &&
                         (currentUser.getRole().getId() == 1 ||
                          "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

        // Non-admins cannot use "All/Public" (id == 1), "Area" (id == 2), or "Custom Group" (id == 5)
        List<IntendedViewerGroup> viewerGroups = intendedViewerGroupRepository.findAll().stream()
                .filter(g -> isAdmin || g.getId() != 1)  // hide "All/Public" for non-admins
                .filter(g -> isAdmin || g.getId() != 2)  // hide "Area" for non-admins
                .filter(g -> isAdmin || g.getId() != 5)  // hide "Custom Group" for non-admins
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("viewerGroups", viewerGroups);

        // Non-admins don't need the custom group list at all
        if (isAdmin) {
            model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());
        } else {
            model.addAttribute("customViewerGroups", java.util.Collections.emptyList());
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);

        if (isAdmin) {
            model.addAttribute("areas", areaRepository.findAll());
            model.addAttribute("offices", officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
        }
        model.addAttribute("activePage", "upload");
        model.addAttribute("systemUploadDate", LocalDateTime.now());
        model.addAttribute("userHasDepartment", currentUser.getDepartment() != null);

        return "uploadform";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleFileUpload(
            @ModelAttribute UploadForm form,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Map<String, String> response = new HashMap<>();

        try {

            /* =========================
            1. AUTHENTICATION CHECK
            ========================== */
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
            if (currentUser == null) {
                response.put("status", "error");
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }


            /* =========================
            2. FILE VALIDATION
            ========================== */
            if (file == null || file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No file selected");
                return ResponseEntity.badRequest().body(response);
            }

            long maxSize = 100 * 1024 * 1024; // 100MB
            if (file.getSize() > maxSize) {
                response.put("status", "error");
                response.put("message", "File size exceeds 100MB limit");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                response.put("status", "error");
                response.put("message", "Invalid file name");
                return ResponseEntity.badRequest().body(response);
            }

            String extension = originalFilename
                    .substring(originalFilename.lastIndexOf('.') + 1)
                    .toLowerCase();

            List<String> allowedExtensions = List.of(
                    "pdf", "doc", "docx", "xls", "xlsx", "txt", "jpg", "jpeg", "png"
            );

            if (!allowedExtensions.contains(extension)) {
                response.put("status", "error");
                response.put("message", "Invalid file extension");
                return ResponseEntity.badRequest().body(response);
            }


            /* =========================
            3. CLASSIFICATION VALIDATION
            ========================== */
            DocumentClassification classification =
                    documentClassificationRepository
                            .findById(form.getDocumentClassificationId())
                            .orElseThrow(() -> new RuntimeException("Invalid classification"));


            /* =========================
            4. VIEWER GROUP VALIDATION
            ========================== */
            IntendedViewerGroup viewerGroup =
                    intendedViewerGroupRepository
                            .findById(form.getIntendedViewerGroup())
                            .orElseThrow(() -> new RuntimeException("Invalid viewer group"));

            // 🔐 Prevent non-admins from selecting restricted viewer groups
            boolean uploaderIsAdminCheck = currentUser.getRole() != null &&
                (currentUser.getRole().getId() == 1 ||
                 "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

            if (!uploaderIsAdminCheck) {
                String vgName = viewerGroup.getName();
                if ("All".equalsIgnoreCase(vgName) || "Public".equalsIgnoreCase(vgName)
                        || viewerGroup.getId() == 1) {
                    response.put("status", "error");
                    response.put("message", "Only administrators can share documents publicly.");
                    return ResponseEntity.badRequest().body(response);
                }
                if ("Area".equalsIgnoreCase(vgName) || viewerGroup.getId() == 2) {
                    response.put("status", "error");
                    response.put("message", "Only administrators can share documents by Area.");
                    return ResponseEntity.badRequest().body(response);
                }
                if ("Custom Group".equalsIgnoreCase(vgName) || viewerGroup.getId() == 5) {
                    response.put("status", "error");
                    response.put("message", "Only administrators can share documents with Custom Groups.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // 🔐 Prevent Department selection if user has no department
            if ("Department".equalsIgnoreCase(viewerGroup.getName())
                    && currentUser.getDepartment() == null) {

                response.put("status", "error");
                response.put("message", "You are not assigned to a department.");
                return ResponseEntity.badRequest().body(response);
            }


            /* =========================
            5. SAVE FILE TO STORAGE
            ========================== */
            String storedFileName = fileStorageService.saveFile(file);


            /* =========================
            6. BUILD ENTITY
            ========================== */
            ImportableInformation info = new ImportableInformation();

            info.setDocumentClassification(classification);
            info.setTitle(form.getTitle());
            info.setDescription(form.getDescription());
            info.setFilename(storedFileName);
            info.setOriginalFilename(originalFilename);
            info.setFileSizeBytes(file.getSize());
            info.setUploadDate(LocalDateTime.now());
            info.setUploadedBy(currentUser);

            // Document Date
            if (form.getDateCreated() != null) {
                info.setDocumentDate(form.getDateCreated().atStartOfDay());
            } else {
                info.setDocumentDate(LocalDateTime.now());
            }

            info.setIntendedViewerGroup(viewerGroup);

            // DTS Number (optional)
            if (form.getDtsNumber() != null && !form.getDtsNumber().trim().isEmpty()) {
                info.setDtsNumber(form.getDtsNumber().trim());
            }

            // Contract dates (optional — only set when classification is a Contract type)
            if (form.getContractStartDate() != null) {
                info.setContractStartDate(form.getContractStartDate());
            }
            if (form.getContractEndDate() != null) {
                info.setContractEndDate(form.getContractEndDate());
            }

            /* =========================
            6b. TARGET ORG (snapshot)
            ========================== */
            boolean uploaderIsAdmin = currentUser.getRole() != null &&
                (currentUser.getRole().getId() == 1 ||
                 "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

            if (uploaderIsAdmin && form.getTargetOfficeId() != null) {
                // Admin explicitly chose a target office — resolve area from it
                officeRepository.findById(form.getTargetOfficeId()).ifPresent(office -> {
                    info.setTargetOffice(office);
                    info.setTargetArea(office.getArea());
                });
            } else {
                // Regular user (or admin who left target blank) — use their own office
                info.setTargetOffice(currentUser.getOffice());
                info.setTargetArea(currentUser.getOffice() != null ? currentUser.getOffice().getArea() : null);
            }

            if (uploaderIsAdmin && form.getTargetDepartmentId() != null) {
                departmentRepository.findById(form.getTargetDepartmentId()).ifPresent(info::setTargetDepartment);
            } else {
                info.setTargetDepartment(currentUser.getDepartment());
            }


            /* =========================
            7. CUSTOM GROUP VALIDATION
            ========================== */
            if ("Custom Group".equalsIgnoreCase(viewerGroup.getName())) {

                Integer customGroupId = form.getCustomViewerGroupId();

                if (customGroupId == null) {
                    response.put("status", "error");
                    response.put("message", "Please select a custom viewer group.");
                    return ResponseEntity.badRequest().body(response);
                }

                CustomViewerGroup customGroup =
                        customViewerGroupService
                                .getGroupById(customGroupId)
                                .orElseThrow(() -> new RuntimeException("Custom viewer group not found"));

                info.setCustomViewerGroup(customGroup);

            } else {
                info.setCustomViewerGroup(null);
            }


            /* =========================
            8. TAG PROCESSING
            ========================== */
            if (form.getTags() != null && !form.getTags().trim().isEmpty()) {
                Set<Tag> tags = tagService.parseTags(form.getTags());
                info.setTags(tags);
            }


            /* =========================
            9. SAVE DOCUMENT
            ========================== */
            ImportableInformation savedDocument =
                    importableInformationRepository.save(info);


            /* =========================
            10. SEND NOTIFICATIONS
            ========================== */
            try {
                notificationService.notifyDocumentShared(savedDocument);
            } catch (Exception e) {
                logger.error("Notification failed for document {}: {}",
                        savedDocument.getDocumentId(),
                        e.getMessage());
            }


            /* =========================
            11. SUCCESS RESPONSE
            ========================== */
            response.put("status", "success");
            response.put("message", "Document uploaded successfully!");
            response.put("redirectUrl", "/myfiles");

            return ResponseEntity.ok(response);


        } catch (Exception e) {

            logger.error("Error during file upload:", e);

            response.put("status", "error");
            response.put("message", "An unexpected error occurred while uploading the document.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ADD NEW ENDPOINT for tag autocomplete
    @GetMapping("/api/tags/autocomplete")
    @ResponseBody
    public List<String> getTagAutocomplete(@RequestParam(required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return tagService.getMostUsedTags()
                .stream()
                .map(Tag::getTagName)
                .limit(10)
                .collect(Collectors.toList());
        }
        
        return tagService.searchTags(q)
            .stream()
            .map(Tag::getTagName)
            .limit(10)
            .collect(Collectors.toList());
    }
    
}