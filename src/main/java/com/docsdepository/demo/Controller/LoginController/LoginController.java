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

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UsersRepository usersRepository;

    // Authentication check - call this from other controllers if needed
    public Users getAuthenticatedUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        return usersRepository.findByIdWithOfficeHierarchy(userId);
    }

    @GetMapping("/logout")
    public String loggingOut(HttpSession session) {
        session.invalidate();
        return "redirect:/Userlogin?logout";
    }

    @GetMapping("/Userlogin")
    public String loggingIn(HttpSession session, @RequestParam(required = false) String logout) {
        // If already logged in, redirect to dashboard
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        Optional<Users> optionalUser = usersRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        Users user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        usersRepository.save(user);

        // Set session
        session.setAttribute("userId", user.getUserId());

        return "redirect:/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}