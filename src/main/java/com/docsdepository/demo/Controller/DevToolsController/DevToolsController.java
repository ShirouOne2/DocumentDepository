package com.docsdepository.demo.Controller.DevToolsController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.docsdepository.demo.Repository.DocumentClassificationRepository;

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
        return "devTools";
    }

    // ========== USER MANAGEMENT ==========
    @PostMapping("/devtools/users/update")
    public String updateUser(
            @RequestParam("userId") Integer userId,
            @RequestParam("positionId") Integer positionId,
            @RequestParam("officeId") Integer officeId,
            @RequestParam("roleId") Integer roleId,
            RedirectAttributes redirectAttributes) {
        
        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if user is admin - prevent editing admin accounts
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
            
            // DO NOT UPDATE USERNAME - it's used as FK in other tables
            user.setJobPosition(position);
            user.setOffice(office);
            user.setRole(role);
            
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
            RedirectAttributes redirectAttributes) {
        
        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if user is admin - prevent resetting admin passwords
            if (user.getRole().getName().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Cannot reset password for admin accounts!");
                return "redirect:/devtools";
            }
            
            // Encode and save new password
            user.setPassword(passwordEncoder.encode(newPassword));
            usersRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "Password reset successfully for user: " + user.getUsername());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resetting password: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    // ========== JOB POSITION MANAGEMENT ==========
    @PostMapping("/devtools/positions/add")
    public String addPosition(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String updatePosition(
            @RequestParam("positionId") Integer positionId,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String deletePosition(
            @RequestParam("positionId") Integer positionId,
            RedirectAttributes redirectAttributes) {
        
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
    public String addDepartment(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
        try {
            Department department = new Department();
            department.setName(name);
            departmentRepository.save(department);
            redirectAttributes.addFlashAttribute("success", "Department added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding department: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/departments/update")
    public String updateDepartment(
            @RequestParam("departmentId") Integer departmentId,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            department.setName(name);
            departmentRepository.save(department);
            redirectAttributes.addFlashAttribute("success", "Department updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating department: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/departments/delete")
    public String deleteDepartment(
            @RequestParam("departmentId") Integer departmentId,
            RedirectAttributes redirectAttributes) {
        
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
    public String addArea(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String updateArea(
            @RequestParam("areaId") Integer areaId,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String deleteArea(
            @RequestParam("areaId") Integer areaId,
            RedirectAttributes redirectAttributes) {
        
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
    public String addOffice(
            @RequestParam("officeName") String officeName,
            @RequestParam("departmentId") Integer departmentId,
            @RequestParam("areaId") Integer areaId,
            RedirectAttributes redirectAttributes) {
        
        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            Area area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new RuntimeException("Area not found"));
            
            Office office = new Office();
            office.setOfficeName(officeName);
            office.setDepartment(department);
            office.setArea(area);
            officeRepository.save(office);
            redirectAttributes.addFlashAttribute("success", "Office added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding office: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/offices/update")
    public String updateOffice(
            @RequestParam("officeId") Integer officeId,
            @RequestParam("officeName") String officeName,
            @RequestParam("departmentId") Integer departmentId,
            @RequestParam("areaId") Integer areaId,
            RedirectAttributes redirectAttributes) {
        
        try {
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Office not found"));
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            Area area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new RuntimeException("Area not found"));
            
            office.setOfficeName(officeName);
            office.setDepartment(department);
            office.setArea(area);
            officeRepository.save(office);
            redirectAttributes.addFlashAttribute("success", "Office updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating office: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/offices/delete")
    public String deleteOffice(
            @RequestParam("officeId") Integer officeId,
            RedirectAttributes redirectAttributes) {
        
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
    public String addViewerGroup(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String updateViewerGroup(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
        try {
            IntendedViewerGroup group = ivgRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Viewer Group not found"));
            group.setName(name);
            ivgRepository.save(group);
            redirectAttributes.addFlashAttribute("success", "Viewer Group updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating viewer group: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/viewergroups/delete")
    public String deleteViewerGroup(
            @RequestParam("groupId") Integer groupId,
            RedirectAttributes redirectAttributes) {
        
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
    public String addClassification(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
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
    public String updateClassification(
            @RequestParam("classificationId") Integer classificationId,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        
        try {
            DocumentClassification classification = docClassRepository.findById(classificationId)
                    .orElseThrow(() -> new RuntimeException("Classification not found"));
            classification.setName(name);
            docClassRepository.save(classification);
            redirectAttributes.addFlashAttribute("success", "Document Classification updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating classification: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }

    @PostMapping("/devtools/classifications/delete")
    public String deleteClassification(
            @RequestParam("classificationId") Integer classificationId,
            RedirectAttributes redirectAttributes) {
        
        try {
            docClassRepository.deleteById(classificationId);
            redirectAttributes.addFlashAttribute("success", "Document Classification deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting classification: " + e.getMessage());
        }
        
        return "redirect:/devtools";
    }
}