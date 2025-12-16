package com.docsdepository.demo.Controller.LoginController;

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


    @GetMapping("/logout")
    public String loggingOut(HttpSession session) {
        session.invalidate(); // clears the session
        return "redirect:/Userlogin?logout";
    }

    @GetMapping("/Userlogin")
    public String loggingIn(HttpSession session) {
        System.out.println("DEBUG: session user = " + session.getAttribute("user"));
        if (session.getAttribute("user") != null) {
            return "redirect:/";
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

        session.setAttribute("user", user); // ✅ this is crucial

        return "redirect:/";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    
    
}
