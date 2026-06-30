package com.docsdepository.demo.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalViewAttributes {
    @Autowired
    private UsersRepository usersRepository;

    @ModelAttribute
    public void addUserFlags(Model model, HttpServletRequest request, HttpSession session) {

        String uri = request.getRequestURI();

        // 🚫 NEVER attach user on auth pages
        if (
            uri.equals("/Userlogin") ||
            uri.equals("/login") ||
            uri.equals("/session-timeout")
        ) {
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return;

        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (user == null) return;

        model.addAttribute("currentUser", user);

        boolean isAdmin = user.getRole() != null &&
                        "ADMIN".equalsIgnoreCase(user.getRole().getName());

        model.addAttribute("isAdmin", isAdmin);
    }
}