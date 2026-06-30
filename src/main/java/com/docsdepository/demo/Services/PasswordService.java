package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single source of truth for all password operations.
 *
 * All controllers and services that need to set or verify passwords
 * must go through this service — never encode/save passwords directly.
 */
@Service
public class PasswordService {

    private static final Logger log = Logger.getLogger(PasswordService.class.getName());

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Validate and change a user's own password (requires current password).
     *
     * @return result object describing success or the specific failure reason
     */
    @Transactional
    public PasswordResult changePassword(Users user,
                                         String currentPassword,
                                         String newPassword,
                                         String confirmPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return PasswordResult.failure("Current password is incorrect.");
        }

        PasswordResult validation = validateNewPassword(newPassword, confirmPassword);
        if (!validation.isSuccess()) {
            return validation;
        }

        applyNewPassword(user, newPassword);
        log.log(Level.INFO, "Password changed by user: {0}", user.getUsername());
        return PasswordResult.success("Password updated successfully.");
    }

    /**
     * Forcefully reset a user's password (admin or OTP flow — no current password needed).
     *
     * @return result object describing success or the specific failure reason
     */
    @Transactional
    public PasswordResult resetPassword(Users user, String newPassword, String confirmPassword) {
        PasswordResult validation = validateNewPassword(newPassword, confirmPassword);
        if (!validation.isSuccess()) {
            return validation;
        }

        applyNewPassword(user, newPassword);
        log.log(Level.INFO, "Password reset for user: {0}", user.getUsername());
        return PasswordResult.success("Password reset successfully.");
    }

    /**
     * Check whether a raw password matches the user's stored hash.
     * Useful for authentication checks outside Spring Security.
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private PasswordResult validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return PasswordResult.failure(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return PasswordResult.failure("Passwords do not match.");
        }
        return PasswordResult.success(null);
    }

    private void applyNewPassword(Users user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        usersRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    public static class PasswordResult {
        private final boolean success;
        private final String message;

        private PasswordResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static PasswordResult success(String message) {
            return new PasswordResult(true, message);
        }

        public static PasswordResult failure(String message) {
            return new PasswordResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}