package com.docsdepository.demo.Controller.UploadModalController;

import com.docsdepository.demo.Controller.DTO.UploadForm;
import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
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
        // Get logged-in user
        Users currentUser = (Users) session.getAttribute("user");
        
        // Fetch all classifications
        List<DocumentClassification> classifications = documentClassificationRepository.findAll();
        
        model.addAttribute("uploadForm", new UploadForm());
        model.addAttribute("classifications", classifications);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "upload");
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
            // Get current user from session
            Users currentUser = (Users) session.getAttribute("user");
            if (currentUser == null) {
                response.put("status", "error");
                response.put("message", "User not logged in");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 1. Save file
            String finalFileName = fileStorageService.saveFile(file);

            // 2. Resolve FK: document_classification
            DocumentClassification classification =
                    documentClassificationRepository
                            .findById(form.getDocumentClassificationId())
                            .orElseThrow(() -> new RuntimeException("Invalid classification"));

            // 3. Build entity
            ImportableInformation info = new ImportableInformation();
            info.setDocumentClassification(classification);
            info.setTitle(form.getTitle());
            info.setDescription(form.getDescription());
            info.setFilename(finalFileName);
            info.setUploadDate(LocalDateTime.now());
            info.setDateCreated(LocalDateTime.now());
            info.setUploadedBy(currentUser); // ✅ Set the uploader
            info.setIntendedViewerGroup(form.getIntendedViewerGroup()); // ✅ Set viewer group

            // 4. Save entity
            importableInformationRepository.save(info);

            response.put("status", "success");
            response.put("message", "Document uploaded successfully!");
            response.put("redirectUrl", "/files");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}