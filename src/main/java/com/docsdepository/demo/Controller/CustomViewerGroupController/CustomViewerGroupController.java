package com.docsdepository.demo.Controller.CustomViewerGroupController;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.CustomViewerGroupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing custom viewer groups
 * Admin-only functionality
 */
@Controller
@RequestMapping("/admin/custom-viewer-groups")
public class CustomViewerGroupController {

    @Autowired
    private CustomViewerGroupService groupService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Show custom viewer groups management page
     */
    @GetMapping
    public String viewerGroupsPage(Model model, HttpSession session) {
        Users currentUser = getAuthenticatedAdmin(session);
        if (currentUser == null) {
            return "redirect:/access-denied";
        }

        List<CustomViewerGroup> groups = groupService.getAllGroups();
        List<Users> allUsers = usersRepository.findAll();

        model.addAttribute("groups", groups);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "custom-viewer-groups");

        return "custom-viewer-groups";
    }

    /**
     * Create a new custom viewer group
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestParam String groupName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<Integer> memberIds,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Set<Integer> memberSet = memberIds != null ? new HashSet<>(memberIds) : new HashSet<>();
            CustomViewerGroup group = groupService.createGroup(groupName, description, currentUser, memberSet);

            response.put("success", true);
            response.put("message", "Custom viewer group created successfully");
            response.put("groupId", group.getGroupId());
            response.put("groupName", group.getGroupName());
            response.put("memberCount", group.getMemberCount());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update an existing custom viewer group
     */
    @PostMapping("/{groupId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable Integer groupId,
            @RequestParam String groupName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<Integer> memberIds,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Set<Integer> memberSet = memberIds != null ? new HashSet<>(memberIds) : new HashSet<>();
            CustomViewerGroup group = groupService.updateGroup(groupId, groupName, description, memberSet);

            response.put("success", true);
            response.put("message", "Group updated successfully");
            response.put("memberCount", group.getMemberCount());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a custom viewer group
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

            groupService.deleteGroup(groupId);

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
     * Get group details (for editing modal)
     */
    @GetMapping("/{groupId}/details")
    @ResponseBody
    public ResponseEntity<?> getGroupDetails(@PathVariable Integer groupId, HttpSession session) {
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<CustomViewerGroup> groupOpt = groupService.getGroupById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CustomViewerGroup group = groupOpt.get();
            
            Map<String, Object> details = new HashMap<>();
            details.put("groupId", group.getGroupId());
            details.put("groupName", group.getGroupName());
            details.put("description", group.getDescription());
            details.put("isActive", group.getIsActive());
            details.put("memberCount", group.getMemberCount());
            details.put("createdBy", group.getCreatedBy().getUsername());
            details.put("createdAt", group.getCreatedAt());
            
            // Get member IDs
            List<Integer> memberIds = group.getMembers().stream()
                    .map(Users::getUserId)
                    .collect(Collectors.toList());
            details.put("memberIds", memberIds);

            // Get member details for display
            List<Map<String, Object>> members = group.getMembers().stream()
                    .map(user -> {
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("userId", user.getUserId());
                        memberInfo.put("username", user.getUsername());
                        memberInfo.put("office", user.getOffice().getOfficeName());
                        return memberInfo;
                    })
                    .collect(Collectors.toList());
            details.put("members", members);

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add member to group
     */
    @PostMapping("/{groupId}/add-member")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable Integer groupId,
            @RequestParam Integer userId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            groupService.addMember(groupId, userId);

            response.put("success", true);
            response.put("message", "Member added successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding member: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Remove member from group
     */
    @PostMapping("/{groupId}/remove-member")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable Integer groupId,
            @RequestParam Integer userId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            groupService.removeMember(groupId, userId);

            response.put("success", true);
            response.put("message", "Member removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error removing member: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /admin/custom-viewer-groups/org-structure
     *
     * Returns all areas, offices, departments, and active users in one payload
     * so the modal bulk-add tab can filter client-side without extra round-trips.
     */
    @GetMapping("/org-structure")
    @ResponseBody
    public ResponseEntity<?> getOrgStructure(HttpSession session) {
        try {
            Users currentUser = getAuthenticatedAdmin(session);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Map<String, Object>> areas = areaRepository.findAll().stream()
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",   a.getId());
                    m.put("name", a.getName());
                    return m;
                })
                .collect(Collectors.toList());

            List<Map<String, Object>> offices = officeRepository.findAll().stream()
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",     o.getOfficeId());
                    m.put("name",   o.getOfficeName());
                    m.put("areaId", o.getArea() != null ? o.getArea().getId() : null);
                    return m;
                })
                .collect(Collectors.toList());

            List<Map<String, Object>> departments = departmentRepository.findAll().stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",       d.getId());
                    m.put("name",     d.getName());
                    m.put("officeId", d.getOffice() != null ? d.getOffice().getOfficeId() : null);
                    return m;
                })
                .collect(Collectors.toList());

            List<Map<String, Object>> users = usersRepository.findAllWithDetails().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId",       u.getUserId());
                    m.put("username",     u.getUsername());
                    m.put("officeId",     u.getOffice() != null ? u.getOffice().getOfficeId() : null);
                    m.put("officeName",   u.getOffice() != null ? u.getOffice().getOfficeName() : null);
                    m.put("areaId",       u.getOffice() != null && u.getOffice().getArea() != null
                                              ? u.getOffice().getArea().getId() : null);
                    m.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
                    return m;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("areas",       areas);
            response.put("offices",     offices);
            response.put("departments", departments);
            response.put("users",       users);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method
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