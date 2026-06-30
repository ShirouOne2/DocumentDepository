package com.docsdepository.demo.Controller.DevToolsController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Entity.JobPosition;
import com.docsdepository.demo.Entity.Department;
import com.docsdepository.demo.Entity.Office;
import com.docsdepository.demo.Entity.Area;
import com.docsdepository.demo.Entity.UserRoles;
import com.docsdepository.demo.Entity.IntendedViewerGroup;
import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Repository.IntendedViewerGroupRepository;
import com.docsdepository.demo.Repository.JobPositionRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.UserRolesRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.PasswordService;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DevToolsController {

    @Autowired private UsersRepository usersRepository;
    @Autowired private UserRolesRepository rolesRepository;
    @Autowired private JobPositionRepository jobPositionRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private OfficeRepository officeRepository;
    @Autowired private IntendedViewerGroupRepository ivgRepository;
    @Autowired private DocumentClassificationRepository docClassRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private PasswordService passwordService;

    @GetMapping("/devtools")
    public String devTools(Model model) {
        model.addAttribute("users", usersRepository.findAll());
        model.addAttribute("roles", rolesRepository.findAll());
        model.addAttribute("jobPositions", jobPositionRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("areas", areaRepository.findAll());
        model.addAttribute("offices", officeRepository.findAll());
        model.addAttribute("viewerGroups", ivgRepository.findAll());
        model.addAttribute("documentClassifications", docClassRepository.findAll());
        model.addAttribute("activePage", "devtools");
        return "devtools";
    }

    // ========== API: Departments by Office (for cascading dropdown) ==========

    /**
     * Returns departments belonging to a given office as JSON.
     * Used by devtools and uploadform JS to populate department dropdown
     * after an office is selected. Returns empty array if office has no
     * departments — JS should then disable the department field.
     */
    @GetMapping("/api/departments/by-office/{officeId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDepartmentsByOffice(
            @PathVariable Integer officeId) {

        List<Map<String, Object>> result = departmentRepository
                .findByOfficeId(officeId)
                .stream()
                .map(d -> Map.of(
                        "id", (Object) d.getId(),
                        "name", (Object) d.getName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ========== USER MANAGEMENT ==========

    @PostMapping("/devtools/users/add")
    public String addUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Integer roleId,
            @RequestParam Integer positionId,
            @RequestParam Integer officeId,
            @RequestParam(required = false) Integer departmentId,  // ✅ Optional
            RedirectAttributes redirectAttributes) {

        try {
            if (usersRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/devtools";
            }

            UserRoles role = rolesRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            if ("ADMIN".equals(role.getName())) {
                redirectAttributes.addFlashAttribute("error",
                    "Admin accounts cannot be created via DevTools.");
                return "redirect:/devtools";
            }

            JobPosition position = jobPositionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("Job position not found"));
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Office not found"));

            // ✅ Department is optional — null if office has no departments
            Department department = null;
            if (departmentId != null) {
                department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new RuntimeException("Department not found"));
            }

            Users user = new Users();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setJobPosition(position);
            user.setOffice(office);
            user.setDepartment(department);
            user.setLastLogin(null);
            user.setKeywords(null);
            user.setIsActive(true);

            usersRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "User added successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add user: " + e.getMessage());
        }

        return "redirect:/devtools";
    }

    @PostMapping("/devtools/users/update")
    public String updateUser(
        @RequestParam("userId") Integer userId,
        @RequestParam("email") String email,
        @RequestParam("positionId") Integer positionId,
        @RequestParam("officeId") Integer officeId,
        @RequestParam("roleId") Integer roleId,
        @RequestParam(value = "departmentId", required = false) Integer departmentId,  // ✅ Optional
        RedirectAttributes redirectAttributes) {

        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole().getName().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Cannot edit admin accounts!");
                return "redirect:/devtools";
            }

            JobPosition position = jobPositionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Office not found"));
            UserRoles role = rolesRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            // ✅ Department is optional — null clears it if office has no departments
            Department department = null;
            if (departmentId != null) {
                department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new RuntimeException("Department not found"));
            }

            user.setJobPosition(position);
            user.setOffice(office);
            user.setRole(role);
            user.setEmail(email);
            user.setDepartment(department);
            usersRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }

        return "redirect:/devtools";
    }

    @PostMapping("/devtools/users/reset-password")
    public String resetUserPassword(
            @RequestParam("userId") Integer userId,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole().getName().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Cannot reset password for admin accounts!");
                return "redirect:/devtools";
            }

            PasswordService.PasswordResult result = passwordService.resetPassword(user, newPassword, confirmPassword);

            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("success",
                        "Password reset successfully for user: " + user.getUsername());
            } else {
                redirectAttributes.addFlashAttribute("error", result.getMessage());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resetting password: " + e.getMessage());
        }

        return "redirect:/devtools";
    }

    @PostMapping("/devtools/users/archive/{userId}")
    public String archiveUser(@PathVariable Integer userId, RedirectAttributes redirectAttributes) {
        try {
            Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if ("ADMIN".equals(user.getRole().getName())) {
                redirectAttributes.addFlashAttribute("error", "Cannot archive admin accounts!");
                return "redirect:/devtools";
            }

            user.setIsActive(false);
            usersRepository.save(user);
            redirectAttributes.addFlashAttribute("success",
                "User '" + user.getUsername() + "' has been archived successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to archive user: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/users/restore/{userId}")
    public String restoreUser(@PathVariable Integer userId, RedirectAttributes redirectAttributes) {
        try {
            Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            user.setIsActive(true);
            usersRepository.save(user);
            redirectAttributes.addFlashAttribute("success",
                "User '" + user.getUsername() + "' has been restored successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to restore user: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== JOB POSITION MANAGEMENT ==========
    @PostMapping("/devtools/positions/add")
    public String addPosition(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            JobPosition position = new JobPosition();
            position.setName(name);
            jobPositionRepository.save(position);
            redirectAttributes.addFlashAttribute("success", "Position added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding position: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/positions/update")
    public String updatePosition(@RequestParam("positionId") Integer positionId, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            JobPosition position = jobPositionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            position.setName(name);
            jobPositionRepository.save(position);
            redirectAttributes.addFlashAttribute("success", "Position updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating position: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/positions/delete")
    public String deletePosition(@RequestParam("positionId") Integer positionId, RedirectAttributes redirectAttributes) {
        try {
            jobPositionRepository.deleteById(positionId);
            redirectAttributes.addFlashAttribute("success", "Position deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting position: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== DEPARTMENT MANAGEMENT ==========
    @PostMapping("/devtools/departments/add")
    public String addDepartment(@RequestParam("name") String name, @RequestParam("officeId") Integer officeId, RedirectAttributes redirectAttributes) {
        try {
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Office not found"));
            Department department = new Department();
            department.setName(name);
            department.setOffice(office);
            departmentRepository.save(department);
            redirectAttributes.addFlashAttribute("success", "Department added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding department: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/departments/update")
    public String updateDepartment(@RequestParam("departmentId") Integer departmentId, @RequestParam("name") String name, @RequestParam("officeId") Integer officeId, RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Office not found"));
            department.setName(name);
            department.setOffice(office);
            departmentRepository.save(department);
            redirectAttributes.addFlashAttribute("success", "Department updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating department: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/departments/delete")
    public String deleteDepartment(@RequestParam("departmentId") Integer departmentId, RedirectAttributes redirectAttributes) {
        try {
            departmentRepository.deleteById(departmentId);
            redirectAttributes.addFlashAttribute("success", "Department deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting department: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== AREA MANAGEMENT ==========
    @PostMapping("/devtools/areas/add")
    public String addArea(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            Area area = new Area();
            area.setName(name);
            areaRepository.save(area);
            redirectAttributes.addFlashAttribute("success", "Area added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding area: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/areas/update")
    public String updateArea(@RequestParam("areaId") Integer areaId, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            Area area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new RuntimeException("Area not found"));
            area.setName(name);
            areaRepository.save(area);
            redirectAttributes.addFlashAttribute("success", "Area updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating area: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/areas/delete")
    public String deleteArea(@RequestParam("areaId") Integer areaId, RedirectAttributes redirectAttributes) {
        try {
            areaRepository.deleteById(areaId);
            redirectAttributes.addFlashAttribute("success", "Area deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting area: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== OFFICE MANAGEMENT ==========
    @PostMapping("/devtools/offices/add")
    public String addOffice(@RequestParam("officeName") String officeName, @RequestParam("areaId") Integer areaId, RedirectAttributes redirectAttributes) {
        try {
            Area area = areaRepository.findById(areaId).orElseThrow(() -> new RuntimeException("Area not found"));
            Office office = new Office();
            office.setOfficeName(officeName);
            office.setArea(area);
            officeRepository.save(office);
            redirectAttributes.addFlashAttribute("success", "Office added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding office: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/offices/update")
    public String updateOffice(@RequestParam("officeId") Integer officeId, @RequestParam("officeName") String officeName, @RequestParam("areaId") Integer areaId, RedirectAttributes redirectAttributes) {
        try {
            Office office = officeRepository.findById(officeId).orElseThrow(() -> new RuntimeException("Office not found"));
            Area area = areaRepository.findById(areaId).orElseThrow(() -> new RuntimeException("Area not found"));
            office.setOfficeName(officeName);
            office.setArea(area);
            officeRepository.save(office);
            redirectAttributes.addFlashAttribute("success", "Office updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating office: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/offices/delete")
    public String deleteOffice(@RequestParam("officeId") Integer officeId, RedirectAttributes redirectAttributes) {
        try {
            officeRepository.deleteById(officeId);
            redirectAttributes.addFlashAttribute("success", "Office deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting office: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== INTENDED VIEWER GROUP MANAGEMENT ==========
    @PostMapping("/devtools/viewergroups/add")
    public String addViewerGroup(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            IntendedViewerGroup group = new IntendedViewerGroup();
            group.setName(name);
            ivgRepository.save(group);
            redirectAttributes.addFlashAttribute("success", "Viewer Group added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding viewer group: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/viewergroups/update")
    public String updateViewerGroup(@RequestParam("groupId") Integer groupId, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            IntendedViewerGroup group = ivgRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Viewer Group not found"));
            group.setName(name);
            ivgRepository.save(group);
            redirectAttributes.addFlashAttribute("success", "Viewer Group updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating viewer group: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/viewergroups/delete")
    public String deleteViewerGroup(@RequestParam("groupId") Integer groupId, RedirectAttributes redirectAttributes) {
        try {
            ivgRepository.deleteById(groupId);
            redirectAttributes.addFlashAttribute("success", "Viewer Group deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting viewer group: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    // ========== DOCUMENT CLASSIFICATION MANAGEMENT ==========
    @PostMapping("/devtools/classifications/add")
    public String addClassification(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            DocumentClassification classification = new DocumentClassification();
            classification.setName(name);
            docClassRepository.save(classification);
            redirectAttributes.addFlashAttribute("success", "Document Classification added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding classification: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/classifications/update")
    public String updateClassification(@RequestParam("classificationId") Integer classificationId, @RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            DocumentClassification classification = docClassRepository.findById(classificationId).orElseThrow(() -> new RuntimeException("Classification not found"));
            classification.setName(name);
            docClassRepository.save(classification);
            redirectAttributes.addFlashAttribute("success", "Document Classification updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating classification: " + e.getMessage());
        }
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/classifications/delete")
    public String deleteClassification(@RequestParam("classificationId") Integer classificationId, RedirectAttributes redirectAttributes) {
        try {
            docClassRepository.deleteById(classificationId);
            redirectAttributes.addFlashAttribute("success", "Document Classification deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting classification: " + e.getMessage());
        }
        return "redirect:/devtools";
    }
}