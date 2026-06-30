package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.PasswordResetToken;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.PasswordResetTokenRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;  // Use your existing one
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PasswordResetService {

    private static final Logger log = Logger.getLogger(PasswordResetService.class.getName());
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder; 

    @Autowired
    private PasswordService passwordService;

    @Value("${app.otp.expiry-minutes:15}")
    private int otpExpiryMinutes;

    /**
     * Generate and send OTP for password reset
     */
    @Transactional
    public boolean initiatePasswordReset(String username, String email) {
        try {
            // Find user by username AND email (both must match)
            Optional<Users> userOpt = usersRepository.findByUsernameAndEmail(username, email);
            
            if (userOpt.isEmpty()) {
                log.log(Level.WARNING, "Password reset failed - invalid username/email combination");
                return false;
            }
            
            Users user = userOpt.get();
            
            //Prevent archived users from resetting password
            if (user.getIsActive() == null || !user.getIsActive()) {
                log.log(Level.WARNING, "Password reset failed - account is archived: " + username);
                return false;
            }
            
            // Delete any existing unused tokens for this user
            tokenRepository.deleteByUser(user);
            
            // Generate 6-digit OTP
            String otp = generateOtp();
            
            // Create token with expiry time
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
            PasswordResetToken token = new PasswordResetToken(user, otp, expiryTime);
            tokenRepository.save(token);
            
            // Send OTP to user
            emailService.sendOtpEmail(email, username, otp);
            
            // Notify admin
            emailService.notifyAdminPasswordReset(username, email);
            
            log.log(Level.INFO, "Password reset initiated for user: " + username);
            return true;
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error initiating password reset", e);
            return false;
        }
    }

    /**
     * Verify OTP and reset password
     */
    @Transactional
    public boolean resetPassword(String otp, String newPassword) {
        try {
            // Find valid token
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenAndIsUsedFalse(otp);
            
            if (tokenOpt.isEmpty()) {
                log.log(Level.WARNING, "Invalid or already used OTP");
                return false;
            }
            
            PasswordResetToken token = tokenOpt.get();
            
            // Check if token is expired
            if (token.isExpired()) {
                log.log(Level.WARNING, "Expired OTP");
                return false;
            }
            
            // Update user password with BCrypt encoding
            Users user = token.getUser();
            
            // ✅ ADD THIS CHECK - Prevent archived users from resetting password
            if (user.getIsActive() == null || !user.getIsActive()) {
                log.log(Level.WARNING, "Password reset failed - account is archived: " + user.getUsername());
                return false;
            }
            
            PasswordService.PasswordResult result = passwordService.resetPassword(user, newPassword, newPassword);
            // confirmPassword == newPassword here because OTP flow already validated match
            // in PasswordResetController before calling this service.
            if (!result.isSuccess()) {
                log.log(Level.WARNING, "Password reset rejected by PasswordService: {0}", result.getMessage());
                return false;
            }
            
            // Mark token as used
            token.setIsUsed(true);
            tokenRepository.save(token);
            
            // Send confirmation email
            emailService.sendPasswordResetConfirmation(user.getEmail(), user.getUsername());
            
            log.log(Level.INFO, "Password reset successful for user: " + user.getUsername());
            return true;
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error resetting password", e);
            return false;
        }
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Clean up expired tokens (can be run as scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
        log.log(Level.INFO, "Cleaned up expired password reset tokens");
    }
}