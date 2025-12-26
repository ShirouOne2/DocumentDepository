package com.docsdepository.demo.Controller.SettingsController;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public String settings(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (user == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }

        model.addAttribute("user", user);
        model.addAttribute("activePage", "settings");

        return "settings";
    }

    @PostMapping("/password")
    @ResponseBody
    public Map<String, Object> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.put("status", "error");
            response.put("message", "Session expired. Please log in again.");
            return response;
        }

        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (user == null) {
            response.put("status", "error");
            response.put("message", "User not found.");
            return response;
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            response.put("status", "error");
            response.put("message", "Current password is incorrect.");
            return response;
        }

        if (!newPassword.equals(confirmPassword)) {
            response.put("status", "error");
            response.put("message", "New passwords do not match.");
            return response;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        response.put("status", "success");
        response.put("message", "Password updated successfully.");

        return response;
    }
}