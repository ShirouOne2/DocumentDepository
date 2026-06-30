package com.docsdepository.demo.Controller.FileController;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.FileIntegrityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class FileIntegrityController {

    @Autowired
    private FileIntegrityService fileIntegrityService;

    @Autowired
    private UsersRepository usersRepository;

    private static final int PAGE_SIZE = 50;

    /**
     * Show file integrity dashboard
     */
    @GetMapping("/file-integrity")
    public String fileIntegrityDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Model model, 
            HttpSession session) {
        
        // Check if user is admin
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null || currentUser.getRole() == null ||
            (currentUser.getRole().getId() != 1 && !"ADMIN".equalsIgnoreCase(currentUser.getRole().getName()))) {
            return "redirect:/access-denied";
        }

        // Get integrity report
        FileIntegrityService.IntegrityReport report = fileIntegrityService.getIntegrityReport();
        
        // Get paginated list of broken files (with optional search)
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<FileIntegrityService.FileStatus> brokenFilesPage;
        
        if (search != null && !search.trim().isEmpty()) {
            brokenFilesPage = fileIntegrityService.searchBrokenFiles(search, pageable);
        } else {
            brokenFilesPage = fileIntegrityService.findBrokenFiles(pageable);
        }

        model.addAttribute("report", report);
        model.addAttribute("brokenFiles", brokenFilesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", brokenFilesPage.getTotalPages());
        model.addAttribute("totalItems", brokenFilesPage.getTotalElements());
        model.addAttribute("activePage", "file-integrity");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("search", search);

        return "file-integrity";
    }

    /**
     * Archive a single broken file
     */
    @PostMapping("/file-integrity/archive/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveBrokenFile(
            @PathVariable Integer documentId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin permission
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                response.put("status", "error");
                response.put("message", "Not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            Users currentUser = usersRepository.findById(userId).orElse(null);
            if (currentUser == null || currentUser.getRole() == null ||
                (currentUser.getRole().getId() != 1 && !"ADMIN".equalsIgnoreCase(currentUser.getRole().getName()))) {
                response.put("status", "error");
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            // Archive the document
            boolean success = fileIntegrityService.archiveBrokenFile(documentId);
            
            if (success) {
                response.put("status", "success");
                response.put("message", "Document archived successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to archive document");
                return ResponseEntity.status(500).body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Archive all broken files
     */
    @PostMapping("/file-integrity/archive-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveAllBrokenFiles(
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check admin permission
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                response.put("status", "error");
                response.put("message", "Not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            Users currentUser = usersRepository.findById(userId).orElse(null);
            if (currentUser == null || currentUser.getRole() == null ||
                (currentUser.getRole().getId() != 1 && !"ADMIN".equalsIgnoreCase(currentUser.getRole().getName()))) {
                response.put("status", "error");
                response.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            // Archive all broken files
            int archivedCount = fileIntegrityService.archiveAllBrokenFiles();
            
            response.put("status", "success");
            response.put("message", "Successfully archived " + archivedCount + " broken file(s)");
            response.put("archivedCount", archivedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}