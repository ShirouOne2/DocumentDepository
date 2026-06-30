package com.docsdepository.demo.Controller.KnowledgeBaseController;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class KnowledgeBaseController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/knowledge-base")
    public String knowledgeBase(Model model, HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (user == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }

        boolean isAdmin = user.getRole() != null &&
                (user.getRole().getId() == 1 ||
                 "ADMIN".equalsIgnoreCase(user.getRole().getName()));

        model.addAttribute("activePage", "knowledge-base");
        model.addAttribute("isAdmin",    isAdmin);
        model.addAttribute("currentUserId", userId);

        return "knowledge-base";
    }
}