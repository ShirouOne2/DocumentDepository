package com.docsdepository.demo.Controller.DashboardController;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Repository.AuditLogRepository;
import com.docsdepository.demo.Services.SearchAnalyticsService;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Controller.LoginController.LoginController;
import com.docsdepository.demo.Config.ActiveSessionCounter;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
    @Autowired private SearchAnalyticsService searchAnalyticsService;
    @Autowired private AuditLogRepository auditLogRepository;  // ✅ ADDED

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model, HttpSession session) {

        // Use LoginController for authentication
        Users currentUser = loginController.getAuthenticatedUser(session);
        if (currentUser == null) {
            return "redirect:/Userlogin";
        }

        // Check role ID 1 (ADMIN) OR role name "ADMIN"
        boolean isAdmin = currentUser.getRole() != null && 
                        (currentUser.getRole().getId() == 1 || 
                        "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

        // Debug logging
        System.out.println("Dashboard - User: " + currentUser.getUsername() + 
                        ", Role ID: " + (currentUser.getRole() != null ? currentUser.getRole().getId() : "NULL") +
                        ", Role Name: " + (currentUser.getRole() != null ? currentUser.getRole().getName() : "NULL") +
                        ", isAdmin: " + isAdmin);

        // === STATISTICS ===
        long totalUploads = importableInformationRepository.count();
        
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long uploadsToday = importableInformationRepository.countUploadsToday(start, end);
        
        // Online users (logged in within last 30 minutes)
        long onlineUsers = ActiveSessionCounter.getLoggedInUserCount();
        System.out.println("📊 Dashboard loaded - Total Sessions: " + onlineUsers + " | Unique Users: " + ActiveSessionCounter.getLoggedInUserCount());
        
        long totalUsers = usersRepository.count();

        //RECENT UPLOADS (Last 10) - FILTERED BY ACCESS
        List<Map<String, Object>> recentUploads;
        
        if (isAdmin) {
            // Admin sees all recent uploads with isOwner flag
            recentUploads = importableInformationRepository
                .findTop10ByOrderByUploadDateDesc()
                .stream()
                .map(doc -> {
                    // Check if current user is the owner
                    boolean isOwner = doc.getUploadedBy().getUserId().equals(currentUser.getUserId());
                    
                    return Map.of(
                        "documentId", (Object) doc.getDocumentId(),
                        "title", (Object) doc.getTitle(),
                        "filename", (Object) doc.getFilename(),
                        "uploadedBy", (Object) doc.getUploadedBy().getUsername(),
                        "uploadDate", (Object) doc.getUploadDate(),
                        "classification", (Object) doc.getDocumentClassification().getName(),
                        "isOwner", (Object) isOwner
                    );
                })
                .collect(Collectors.toList());
        } else {
            // Regular users see only recent uploads they have access to
            Integer officeId = currentUser.getOffice() != null 
                    ? currentUser.getOffice().getOfficeId() 
                    : null;

            Integer areaId = (currentUser.getOffice() != null && 
                            currentUser.getOffice().getArea() != null)
                    ? currentUser.getOffice().getArea().getId()
                    : null;

            Integer departmentId = currentUser.getDepartment() != null
                    ? currentUser.getDepartment().getId()
                    : null;
            
            recentUploads = importableInformationRepository
                .findSharedFiles(
                    currentUser.getUserId(),
                    officeId,
                    areaId,
                    departmentId
                )
                .stream()
                .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                .limit(10)
                .map(doc -> {
                    boolean isOwner = doc.getUploadedBy().getUserId().equals(currentUser.getUserId());
                    
                    return Map.of(
                        "documentId", (Object) doc.getDocumentId(),
                        "title", (Object) doc.getTitle(),
                        "filename", (Object) doc.getFilename(),
                        "uploadedBy", (Object) doc.getUploadedBy().getUsername(),
                        "uploadDate", (Object) doc.getUploadDate(),
                        "classification", (Object) doc.getDocumentClassification().getName(),
                        "isOwner", (Object) isOwner
                    );
                })
                .collect(Collectors.toList());
        }

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

        // === ADMIN-ONLY: RECENT ACTIVITY (COMBINED LOGIN + AUDIT LOGS) ===
        if (isAdmin) {
            List<Map<String, Object>> recentActivity = auditLogRepository
                .findTop100ByOrderByTimestampDesc()
                .stream()
                .limit(20)  // Show last 20 activities
                .map(log -> {
                    // Get user details for position and office
                    Users user = usersRepository.findByUsername(log.getUsername()).orElse(null);
                    
                    String position = (user != null && user.getJobPosition() != null) 
                        ? user.getJobPosition().getName() : null;
                    String office = (user != null && user.getOffice() != null) 
                        ? user.getOffice().getOfficeName() : null;
                    String department = (user != null && user.getDepartment() != null) 
                        ? user.getDepartment().getName() : null;
                    
                    // Convert action to user-friendly format
                    String displayAction = getDisplayAction(log.getAction(), log.getEndpoint());
                    
                    // Get badge CSS class based on action
                    String badgeClass = getBadgeClass(displayAction);
                    
                    return Map.of(
                        "username", (Object) log.getUsername(),
                        "action", (Object) displayAction,
                        "badgeClass", (Object) badgeClass,
                        "endpoint", (Object) (log.getEndpoint() != null ? log.getEndpoint() : "N/A"),
                        "timestamp", (Object) log.getTimestamp(),
                        "ipAddress", (Object) (log.getIpAddress() != null ? log.getIpAddress() : "N/A"),
                        "position", (Object) (position != null ? position : "N/A"),
                        "office", (Object) (office != null ? office : "N/A"),
                        "department", (Object) (department != null ? department : "N/A")
                    );
                })
                .collect(Collectors.toList());
            
            model.addAttribute("recentActivity", recentActivity);
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
        
        if (currentUser == null) {
            return "redirect:/Userlogin";
        }
        
        model.addAttribute("mostSearchedGlobal", searchAnalyticsService.getMostSearchedGlobal());
        model.addAttribute("trendingSearches", searchAnalyticsService.getTrendingSearches());
        model.addAttribute("activePage", "analytics");
        model.addAttribute("currentUser", currentUser);

        return "search-analytics";
    }
    
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public Map<String, Object> getDashboardStats(HttpSession session) {
        Users currentUser = loginController.getAuthenticatedUser(session);
        if (currentUser == null) {
            return Map.of("error", "Not authenticated");
        }

        boolean isAdmin = currentUser.getRole() != null && 
                        (currentUser.getRole().getId() == 1 || 
                        "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

        long totalUploads = importableInformationRepository.count();
        
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long uploadsToday = importableInformationRepository.countUploadsToday(start, end);
        
        long onlineUsers = ActiveSessionCounter.getLoggedInUserCount();
        long totalUsers = usersRepository.count();

        long myUploads = 0;
        if (!isAdmin) {
            myUploads = importableInformationRepository.countByUploadedBy(currentUser);
        }

        return Map.of(
            "totalUploads", totalUploads,
            "uploadsToday", uploadsToday,
            "onlineUsers", onlineUsers,
            "totalUsers", totalUsers,
            "myUploads", myUploads,
            "isAdmin", isAdmin
        );
    }
    /**
     * Convert HTTP method + endpoint to user-friendly action
     */
    private String getDisplayAction(String action, String endpoint) {
        // Handle explicit actions first (LOGIN, LOGOUT, TIMEOUT)
        if ("LOGIN".equals(action)) return "LOGIN";
        if ("LOGOUT".equals(action)) return "LOGOUT";
        if ("TIMEOUT".equals(action)) return "TIMEOUT";
        
        // Map HTTP methods + endpoints to actions
        if (endpoint == null || endpoint.equals("N/A")) {
            return action; // fallback to original action
        }
        
        String lowerEndpoint = endpoint.toLowerCase();
        
        // GET requests
        if ("GET".equals(action)) {
            if (lowerEndpoint.contains("/documents/view/")) return "VIEW DOCUMENT";
            if (lowerEndpoint.contains("/documents/details/")) return "VIEW DETAILS";
            if (lowerEndpoint.contains("/documents/download/")) return "DOWNLOAD";
            if (lowerEndpoint.contains("/myfiles")) return "BROWSE FILES";
            if (lowerEndpoint.contains("/shared")) return "BROWSE SHARED";
            if (lowerEndpoint.contains("/dashboard")) return "VIEW DASHBOARD";
            if (lowerEndpoint.contains("/settings")) return "VIEW SETTINGS";
            return "VIEW";
        }
        
        // POST requests
        if ("POST".equals(action)) {
            if (lowerEndpoint.contains("/upload")) return "UPLOAD";
            if (lowerEndpoint.contains("/edit")) return "EDIT";
            if (lowerEndpoint.contains("/share")) return "SHARE";
            if (lowerEndpoint.contains("/delete")) return "DELETE";
            if (lowerEndpoint.contains("/archive")) return "ARCHIVE";
            if (lowerEndpoint.contains("/comments")) return "COMMENT";
            return "ACTION";
        }
        
        // PUT/PATCH requests
        if ("PUT".equals(action) || "PATCH".equals(action)) {
            return "UPDATE";
        }
        
        // DELETE requests
        if ("DELETE".equals(action)) {
            return "DELETE";
        }
        
        return action; // fallback
    }

    /**
     * Get CSS badge class based on action type
     */
    private String getBadgeClass(String action) {
        if (action == null) return "badge-action";
        
        switch (action) {
            case "LOGIN":
                return "badge-login";
            case "LOGOUT":
                return "badge-logout";
            case "TIMEOUT":
                return "badge-timeout";
            case "UPLOAD":
                return "badge-upload";
            case "VIEW":
            case "VIEW DOCUMENT":
            case "VIEW DETAILS":
            case "BROWSE FILES":
            case "BROWSE SHARED":
            case "VIEW DASHBOARD":
            case "VIEW SETTINGS":
                return "badge-view";
            case "DOWNLOAD":
                return "badge-download";
            case "EDIT":
            case "UPDATE":
                return "badge-edit";
            case "DELETE":
                return "badge-delete";
            case "COMMENT":
                return "badge-action";
            default:
                return "badge-action";
        }
    }
}