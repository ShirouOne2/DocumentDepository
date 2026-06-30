package com.docsdepository.demo.Controller.PasswordResetController;

import com.docsdepository.demo.Services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Show forgot password page
     */
    @GetMapping("/forgot")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    /**
     * Handle forgot password form submission
     */
    @PostMapping("/request")
    public String requestPasswordReset(
            @RequestParam String username,
            @RequestParam String email,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = passwordResetService.initiatePasswordReset(username, email);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", 
                "OTP has been sent to your email. Please check your inbox.");
            return "redirect:/password-reset/verify";
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Invalid username or email combination.");
            return "redirect:/password-reset/forgot";
        }
    }

    /**
     * Show OTP verification page
     */
    @GetMapping("/verify")
    public String showVerifyOtpPage() {
        return "verify-otp";
    }

    /**
     * Handle OTP verification and password reset
     */
    @PostMapping("/reset")
    public String resetPassword(
            @RequestParam String otp,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        // Validate password match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", 
                "Passwords do not match.");
            return "redirect:/password-reset/verify";
        }

        // Validate password strength (optional)
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", 
                "Password must be at least 6 characters long.");
            return "redirect:/password-reset/verify";
        }

        boolean success = passwordResetService.resetPassword(otp, newPassword);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", 
                "Password reset successful. You can now login with your new password.");
            return "redirect:/Userlogin";
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Invalid or expired OTP. Please try again.");
            return "redirect:/password-reset/verify";
        }
    }
}