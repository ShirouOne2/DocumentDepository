package com.docsdepository.demo.Controller.ViewerGroupController;

import com.docsdepository.demo.Entity.*;
import com.docsdepository.demo.Repository.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ⚠️ SIMPLIFIED VERSION - FOR BASIC VIEWER GROUP MANAGEMENT
 * 
 * This controller manages the OLD IntendedViewerGroup system.
 * 
 * For the NEW viewer group system with rules, you need to:
 * 1. Create ViewerGroup, ViewerGroupType, ViewerGroupRule entities
 * 2. Create their repositories
 * 3. Create ViewerGroupService
 * 4. Then use the advanced ViewerGroupController
 */
@Controller
@RequestMapping("/admin/viewer-groups")
public class ViewerGroupController {

    @Autowired
    private IntendedViewerGroupRepository viewerGroupRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private AreaRepository areaRepository;
    
    @Autowired
    private OfficeRepository officeRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private JobPositionRepository positionRepository;

    /**
     * Show viewer groups management page (SIMPLIFIED)
     */
    @GetMapping
    public String viewerGroupsPage(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null || !isAdmin(currentUser)) {
            return "redirect:/access-denied";
        }

        // Get all viewer groups (old system)
        List<IntendedViewerGroup> allGroups = viewerGroupRepository.findAll();

        model.addAttribute("viewerGroups", allGroups);
        model.addAttribute("areas", areaRepository.findAll());
        model.addAttribute("offices", officeRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("positions", positionRepository.findAll());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "viewer-groups");

        return "admin/viewer-groups-simple";
    }

    /**
     * Create a new viewer group (OLD SYSTEM)
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestParam String groupName,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Create simple viewer group
            IntendedViewerGroup group = new IntendedViewerGroup();
            group.setName(groupName);
            
            viewerGroupRepository.save(group);

            response.put("success", true);
            response.put("message", "Viewer group created successfully");
            response.put("groupId", group.getId());
            response.put("groupName", group.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update viewer group name
     */
    @PostMapping("/{groupId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable Integer groupId,
            @RequestParam String groupName,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            IntendedViewerGroup group = viewerGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

            group.setName(groupName);
            viewerGroupRepository.save(group);

            response.put("success", true);
            response.put("message", "Group updated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a viewer group
     */
    @DeleteMapping("/{groupId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteGroup(
            @PathVariable Integer groupId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            viewerGroupRepository.deleteById(groupId);

            response.put("success", true);
            response.put("message", "Group deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get group details
     */
    @GetMapping("/{groupId}/details")
    @ResponseBody
    public ResponseEntity<?> getGroupDetails(@PathVariable Integer groupId, HttpSession session) {
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            IntendedViewerGroup group = viewerGroupRepository.findById(groupId)
                .orElse(null);
            
            if (group == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> details = new HashMap<>();
            details.put("id", group.getId());
            details.put("name", group.getName());

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods
    private Users getAuthenticatedAdmin(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;
        
        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        return (user != null && isAdmin(user)) ? user : null;
    }

    private boolean isAdmin(Users user) {
        return user.getRole() != null && 
               (user.getRole().getId() == 1 || 
                "ADMIN".equalsIgnoreCase(user.getRole().getName()));
    }
}