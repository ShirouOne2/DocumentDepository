package com.docsdepository.demo.Controller.UploadModalController;

import com.docsdepository.demo.Entity.UploadForm;
import com.docsdepository.demo.Repository.UploadFormRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController // Handles REST API requests, returns data (JSON), not views
public class UploadModalController {
    
    private final UploadFormRepository uploadFormRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public UploadModalController(UploadFormRepository uploadFormRepository, FileStorageService fileStorageService) {
        this.uploadFormRepository = uploadFormRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Handles the file upload and metadata saving via AJAX (called from the modal).
     * Returns a JSON response for the JavaScript client to show success/failure via SweetAlert.
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> handleFileUpload(
                                   @ModelAttribute("uploadForm") UploadForm form,
                                   @RequestParam("file") MultipartFile file) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // 1. Save the file to disk
            fileStorageService.saveFile(file);
            
            // 2. Set the filename metadata onto the entity
            form.setFilename(file.getOriginalFilename());
            
            // 3. Save the entity metadata to the database
            uploadFormRepository.save(form);
            
            // Success Response (HTTP 200 OK)
            response.put("message", "Document uploaded and metadata saved successfully!");
            response.put("status", "success");
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (IOException e) {
            // Failure Response (HTTP 500 Internal Server Error)
            response.put("message", "File upload failed due to disk error: " + e.getMessage());
            response.put("status", "error");
            e.printStackTrace();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            // Catch any other exceptions (e.g., database constraints, NullPointerException)
            response.put("message", "An unexpected server error occurred: " + e.getMessage());
            response.put("status", "error");
            e.printStackTrace();
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // NOTE: All other GET mappings were moved to the DashboardController.
}