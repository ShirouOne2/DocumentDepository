package com.docsdepository.demo.Controller.NotificationController;

import com.docsdepository.demo.Entity.Notification;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Services.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ImportableInformationRepository documentRepository;

    /**
     * Show notifications page
     * ✅ SAME PATTERN AS DASHBOARD - Calculate isOwner on-the-fly
     */
    @GetMapping
    public String notificationsPage(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }

        // Get all notifications for the user
        List<Notification> notifications = notificationService.getAllNotifications(currentUser);
        
        // ✅ Calculate documentId and isOwner on-the-fly (SAME AS DASHBOARD)
        List<Map<String, Object>> notificationsWithOwnership = notifications.stream()
            .map(notif -> {
                // Extract document ID from link
                Long documentId = extractDocumentIdFromLink(notif.getLink());
                
                // Calculate isOwner if document exists
                boolean isOwner = false;
                if (documentId != null) {
                    try {
                        ImportableInformation doc = documentRepository.findById(documentId.intValue()).orElse(null);
                        if (doc != null && doc.getUploadedBy() != null) {
                            isOwner = doc.getUploadedBy().getUserId().equals(currentUser.getUserId());
                        }
                    } catch (Exception e) {
                        // If we can't fetch document, default to false
                    }
                }
                
                return Map.of(
                    "notificationId", (Object) notif.getNotificationId(),
                    "type", (Object) notif.getType(),
                    "title", (Object) notif.getTitle(),
                    "message", (Object) notif.getMessage(),
                    "link", (Object) notif.getLink(),
                    "isRead", (Object) notif.getIsRead(),
                    "createdAt", (Object) notif.getCreatedAt(),
                    "documentId", (Object) documentId,  // ✅ Extracted from link
                    "isOwner", (Object) isOwner         // ✅ Calculated on-the-fly
                );
            })
            .collect(Collectors.toList());
        
        model.addAttribute("notifications", notificationsWithOwnership);
        model.addAttribute("activePage", "notifications");
        model.addAttribute("currentUser", currentUser);

        return "notifications";
    }

    /**
     * API: Get unread count
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        Long count = notificationService.getUnreadCount(currentUser);

        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * API: Get recent notifications (for dropdown)
     * ✅ SAME PATTERN AS DASHBOARD - Calculate isOwner on-the-fly
     */
    @GetMapping("/api/recent")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        List<Notification> notifications = notificationService.getRecentNotifications(currentUser);

        // ✅ Calculate documentId and isOwner on-the-fly (SAME AS DASHBOARD)
        List<Map<String, Object>> dtos = notifications.stream()
            .map(notif -> {
                // Extract document ID from link
                Long documentId = extractDocumentIdFromLink(notif.getLink());
                
                // Calculate isOwner if document exists
                boolean isOwner = false;
                if (documentId != null) {
                    try {
                        ImportableInformation doc = documentRepository.findById(documentId.intValue()).orElse(null);
                        if (doc != null && doc.getUploadedBy() != null) {
                            isOwner = doc.getUploadedBy().getUserId().equals(currentUser.getUserId());
                        }
                    } catch (Exception e) {
                        // If we can't fetch document, default to false
                    }
                }
                
                return Map.of(
                    "notificationId", (Object) notif.getNotificationId(),
                    "type", (Object) notif.getType(),
                    "title", (Object) notif.getTitle(),
                    "message", (Object) notif.getMessage(),
                    "link", (Object) notif.getLink(),
                    "isRead", (Object) notif.getIsRead(),
                    "createdAt", (Object) notif.getCreatedAt().toString(),
                    "documentId", (Object) documentId,  // ✅ Extracted from link
                    "isOwner", (Object) isOwner         // ✅ Calculated on-the-fly
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * API: Mark notification as read
     */
    @PostMapping("/api/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Integer id,
            HttpSession session) {
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Not authenticated"));
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        notificationService.markAsRead(id, currentUser);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * API: Mark all as read
     */
    @PostMapping("/api/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAllAsRead(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Not authenticated"));
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        notificationService.markAllAsRead(currentUser);

        return ResponseEntity.ok(Map.of("status", "success", "message", "All notifications marked as read"));
    }

    /**
     * ✅ Helper method to extract document ID from notification link
     * Examples:
     * - "/documents/details/123" -> 123
     * - "/documents/view/456" -> 456
     */
    private Long extractDocumentIdFromLink(String link) {
        if (link == null || !link.contains("/documents/")) {
            return null;
        }
        
        try {
            // Extract last number from URL
            String[] parts = link.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    return Long.parseLong(parts[i]);
                } catch (NumberFormatException e) {
                    // Continue to previous part
                }
            }
        } catch (Exception e) {
            // Return null if parsing fails
        }
        
        return null;
    }
}