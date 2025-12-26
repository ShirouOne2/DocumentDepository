package com.docsdepository.demo.Controller.DashboardController;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.SearchAnalyticsService;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Controller.LoginController.LoginController;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired private ImportableInformationRepository importableInformationRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private DocumentClassificationRepository documentClassificationRepository;
    @Autowired private LoginController loginController;
    
    @Autowired
    private SearchAnalyticsService searchAnalyticsService;

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model, HttpSession session) {

        // Use LoginController for authentication
        Users currentUser = loginController.getAuthenticatedUser(session);
        if (currentUser == null) {
            return "redirect:/Userlogin";
        }

        // CORRECT - checks for role ID 4 which is "ADMIN"
        boolean isAdmin = currentUser.getRole().getId() == 4;

        // === STATISTICS ===
        long totalUploads = importableInformationRepository.count();
        
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long uploadsToday = importableInformationRepository.countUploadsToday(start, end);
        
        // Online users (logged in within last 30 minutes)
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        long onlineUsers = usersRepository.countByLastLoginAfter(thirtyMinutesAgo);
        
        long totalUsers = usersRepository.count();

        // === RECENT UPLOADS (Last 10) ===
        List<Map<String, Object>> recentUploads = importableInformationRepository
        .findTop10ByOrderByUploadDateDesc()
        .stream()
        .map(doc -> Map.of(
            "title", (Object) doc.getTitle(),
            "filename", (Object) doc.getFilename(),
            "uploadedBy", (Object) doc.getUploadedBy().getUsername(),
            "uploadDate", (Object) doc.getUploadDate(),  // CHANGED from dateCreated
            "classification", (Object) doc.getDocumentClassification().getName()
        ))
        .collect(Collectors.toList());

        // === DOCUMENT DISTRIBUTION BY CLASSIFICATION ===
        List<Map<String, Object>> documentsByClassification = documentClassificationRepository
            .findAll()
            .stream()
            .map(classification -> {
                long count = importableInformationRepository.countByDocumentClassification(classification);
                return Map.of(
                    "name", (Object) classification.getName(),
                    "count", (Object) count
                );
            })
            .collect(Collectors.toList());

        // === ADMIN-ONLY: AUDIT LOG (Recent User Activity) ===
        if (isAdmin) {
            List<Map<String, Object>> auditLog = usersRepository
                .findTop20ByOrderByLastLoginDesc()
                .stream()
                .map(user -> Map.of(
                    "username", (Object) user.getUsername(),
                    "position", (Object) user.getJobPosition().getName(),
                    "office", (Object) user.getOffice().getOfficeName(),
                    "lastLogin", (Object) (user.getLastLogin() != null ? user.getLastLogin() : user.getDateCreated())
                ))
                .collect(Collectors.toList());
            
            model.addAttribute("auditLog", auditLog);
        }

        // === MY UPLOADS (For regular users) ===
        if (!isAdmin) {
            long myUploads = importableInformationRepository.countByUploadedBy(currentUser);
            model.addAttribute("myUploads", myUploads);
        }

        // === ADD TO MODEL ===
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard - Document Management System");
        model.addAttribute("totalUploads", totalUploads);
        model.addAttribute("uploadsToday", uploadsToday);
        model.addAttribute("onlineUsers", onlineUsers);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("recentUploads", recentUploads);
        model.addAttribute("documentsByClassification", documentsByClassification);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);

        return "dashboard";
    }
    @GetMapping("/admin/search-analytics")
    public String searchAnalytics(Model model, HttpSession session) {
        Users currentUser = loginController.getAuthenticatedUser(session);
        if (currentUser == null || currentUser.getRole().getId() != 4) {
            return "redirect:/access-denied";
        }

        model.addAttribute("mostSearchedGlobal", searchAnalyticsService.getMostSearchedGlobal());
        model.addAttribute("trendingSearches", searchAnalyticsService.getTrendingSearches());
        model.addAttribute("activePage", "analytics");

        return "search-analytics";
    }
}