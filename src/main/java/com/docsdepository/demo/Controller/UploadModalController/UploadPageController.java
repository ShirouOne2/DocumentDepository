package com.docsdepository.demo.Controller.UploadModalController;

import com.docsdepository.demo.Controller.DTO.UploadForm;
import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.IntendedViewerGroup;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.IntendedViewerGroupRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;

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

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadPageController {

    private final ImportableInformationRepository importableInformationRepository;
    private final DocumentClassificationRepository documentClassificationRepository;
    private final UsersRepository usersRepository;
    private final FileStorageService fileStorageService;
    
    @Autowired
    private IntendedViewerGroupRepository intendedViewerGroupRepository;

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
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }
        
        List<DocumentClassification> classifications = documentClassificationRepository.findAll();
        
        model.addAttribute("uploadForm", new UploadForm());
        model.addAttribute("classifications", classifications);
        model.addAttribute("viewerGroups", intendedViewerGroupRepository.findAll());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "upload");
        model.addAttribute("systemUploadDate", LocalDateTime.now());

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
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No file selected");
                return ResponseEntity.badRequest().body(response);
            }
            
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

            long maxSize = 10 * 1024 * 1024; // 10MB

            if (file.getSize() > maxSize) {
                response.put("status", "error");
                response.put("message", "File size exceeds 10MB limit");
                return ResponseEntity.badRequest().body(response);
            }

            String finalFileName = fileStorageService.saveFile(file);

            DocumentClassification classification =
                    documentClassificationRepository
                            .findById(form.getDocumentClassificationId())
                            .orElseThrow(() -> new RuntimeException("Invalid classification"));

            ImportableInformation info = new ImportableInformation();
            info.setDocumentClassification(classification);
            info.setTitle(form.getTitle());
            info.setDescription(form.getDescription());
            info.setFilename(finalFileName);
            info.setDateCreated(form.getDateCreated().atStartOfDay()); // Convert LocalDate to LocalDateTime
            info.setUploadDate(LocalDateTime.now()); // NEW LINE - system upload timestamp
            info.setUploadedBy(currentUser);
            
            IntendedViewerGroup viewerGroup = intendedViewerGroupRepository
                .findById(form.getIntendedViewerGroup())
                .orElseThrow(() -> new RuntimeException("Invalid viewer group"));

            info.setIntendedViewerGroup(viewerGroup);

            importableInformationRepository.save(info);

            response.put("status", "success");
            response.put("message", "Document uploaded successfully!");
            response.put("redirectUrl", "/myfiles");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred while uploading the document.");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}