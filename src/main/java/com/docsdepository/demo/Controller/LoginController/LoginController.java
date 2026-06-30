package com.docsdepository.demo.Controller.LoginController;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.docsdepository.demo.Config.ActiveSessionCounter;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private AuditService auditService;

    // Authentication check - call this from other controllers if needed
    public Users getAuthenticatedUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        
        // CHECK IF USER IS STILL ACTIVE
        if (user != null && (user.getIsActive() == null || !user.getIsActive())) {
            return null; // Treat as not authenticated if archived
        }
        
        return user;
    }

    @GetMapping("/logout")
    public String loggingOut(HttpSession session, HttpServletRequest request) {
        // 📝 LOG LOGOUT BEFORE INVALIDATING SESSION
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId != null) {
            Users user = usersRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logLogout(userId, user.getUsername(), request);
            }
        }
        
        session.invalidate();
        return "redirect:/Userlogin?logout";
    }

    @GetMapping("/Userlogin")
    public String loggingIn(HttpSession session,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String timeout,
                            HttpServletResponse response,
                            Model model) {

        // Prevent browser from caching login page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // ✅ ALWAYS redirect logged-in users first, regardless of query params
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }

        if (timeout != null) {
            model.addAttribute("error", "Your session expired due to inactivity. Please log in again.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        return "login";
    }
    
    @GetMapping("/login")
    public String redirectLoginGet() {
        return "redirect:/Userlogin";
    }
    
    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            HttpServletResponse response,
                            HttpServletRequest request,
                            Model model) {

        // Add headers to prevent caching
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Optional<Users> optionalUser = usersRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", username);
            return "login";
        }

        Users user = optionalUser.get();

        // CHECK IF USER IS ARCHIVED/INACTIVE
        if (user.getIsActive() == null || !user.getIsActive()) {
            model.addAttribute("error", "This account has been disabled. Please contact the administrator.");
            model.addAttribute("username", username);
            return "login";
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", username);
            return "login";
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        usersRepository.save(user);

        // Set session attributes
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("loginSuccess", true);
        ActiveSessionCounter.registerUserSession(session.getId(), user.getUserId());

        // 📝 LOG LOGIN AFTER SUCCESSFUL AUTHENTICATION
        auditService.logLogin(user.getUserId(), user.getUsername(), request);

        // Redirect to dashboard on success
        return "redirect:/dashboard";
    }
    
    @GetMapping("/session-timeout")
    public String sessionTimeout(HttpSession session, HttpServletRequest request) {
        // 📝 LOG TIMEOUT BEFORE INVALIDATING SESSION
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId != null) {
            Users user = usersRepository.findById(userId).orElse(null);
            if (user != null) {
                auditService.logTimeout(userId, user.getUsername(), request);
            }
        }
        
        session.invalidate();
        return "redirect:/Userlogin?timeout=true";
    }
}