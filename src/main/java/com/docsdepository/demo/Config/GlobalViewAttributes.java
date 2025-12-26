package com.docsdepository.demo.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalViewAttributes {
    @Autowired
    private UsersRepository usersRepository;

    @ModelAttribute
    public void addUserFlags(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId != null) {
            Users user = usersRepository.findByIdWithOfficeHierarchy(userId);

            if (user != null) {
                model.addAttribute("currentUser", user);
                model.addAttribute(
                    "isAdmin",
                    "ADMIN".equals(user.getRole().getName())
                );
            }
        }
    }
}
