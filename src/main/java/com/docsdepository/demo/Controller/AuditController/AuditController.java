package com.docsdepository.demo.Controller.AuditController;

import com.docsdepository.demo.Entity.AuditLog;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.AuditLogRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Controller.LoginController.LoginController;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private LoginController loginController;

    private static final int PAGE_SIZE = 25;

    @GetMapping("/admin/audit-log")
    public String auditLog(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        Users currentUser = loginController.getAuthenticatedUser(session);
        if (currentUser == null) return "redirect:/Userlogin";

        boolean isAdmin = currentUser.getRole() != null &&
                (currentUser.getRole().getId() == 1 ||
                 "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));
        if (!isAdmin) return "redirect:/access-denied";

        // ── Parse filter params ────────────────────────────────────────────
        LocalDateTime startDt = (startDate != null && !startDate.isBlank())
                ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime endDt = (endDate != null && !endDate.isBlank())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        // Blank strings → null so JPQL COALESCE-style IS NULL check works
        String usernameParam = (username != null && !username.isBlank()) ? username : null;
        String actionParam   = (action   != null && !action.isBlank())   ? action   : null;

        // ── Paginated query ────────────────────────────────────────────────
        Page<AuditLog> logPage = auditLogRepository.findFiltered(
                usernameParam, actionParam, startDt, endDt,
                PageRequest.of(page, PAGE_SIZE)
        );

        // ── Stats ──────────────────────────────────────────────────────────
        long totalUsers     = usersRepository.count();
        long activeToday    = auditLogRepository.countDistinctUsersSince(
                                    LocalDate.now().atStartOfDay());
        long activeThisWeek = auditLogRepository.countDistinctUsersSince(
                                    LocalDateTime.now().minusWeeks(1));
        long totalMatching  = auditLogRepository.countFiltered(
                                    usernameParam, actionParam, startDt, endDt);

        // ── Model ──────────────────────────────────────────────────────────
        model.addAttribute("auditLogs",   logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  logPage.getTotalPages());
        model.addAttribute("totalItems",  totalMatching);
        model.addAttribute("totalUsers",  totalUsers);
        model.addAttribute("activeToday", activeToday);
        model.addAttribute("activeWeek",  activeThisWeek);

        model.addAttribute("username",  username);
        model.addAttribute("action",    action);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate",   endDate);
        model.addAttribute("activePage",   "audit");
        model.addAttribute("currentUser",  currentUser);

        return "audit-log";
    }
}