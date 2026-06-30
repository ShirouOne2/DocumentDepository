package com.docsdepository.demo.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ─── Existing methods ────────────────────────────────────────────────────

    public void sendOtpEmail(String toEmail, String username, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset OTP - Document Depository");
            message.setText(
                "Hello " + username + ",\n\n" +
                "You have requested to reset your password.\n\n" +
                "Your One-Time Password (OTP) is: " + otp + "\n\n" +
                "This OTP will expire in 15 minutes.\n\n" +
                "If you did not request this, please ignore this email or contact support.\n\n" +
                "Best regards,\n" +
                "Document Depository Team"
            );
            mailSender.send(message);
            log.log(Level.INFO, "OTP email sent to: " + toEmail);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send OTP email to: " + toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void notifyAdminPasswordReset(String username, String userEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("Password Reset Request - Document Depository");
            message.setText(
                "A password reset request has been made.\n\n" +
                "Username: " + username + "\n" +
                "Email: " + userEmail + "\n" +
                "Time: " + java.time.LocalDateTime.now() + "\n\n" +
                "This is an automated notification."
            );
            mailSender.send(message);
            log.log(Level.INFO, "Admin notification sent for user: " + username);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to send admin notification", e);
        }
    }

    public void sendPasswordResetConfirmation(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Successful - Document Depository");
            message.setText(
                "Hello " + username + ",\n\n" +
                "Your password has been successfully reset.\n\n" +
                "If you did not make this change, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "Document Depository Team"
            );
            mailSender.send(message);
            log.log(Level.INFO, "Password reset confirmation sent to: " + toEmail);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to send confirmation email", e);
        }
    }

    // ─── Contract expiry alert ────────────────────────────────────────────────

    /**
     * Send a contract expiry alert email to the document uploader.
     *
     * @param toEmail       recipient email address
     * @param username      recipient display name
     * @param documentTitle title of the contract document
     * @param documentId    document ID (used to build the deep-link)
     * @param endDate       contract end date
     * @param daysLeft      number of days until expiry (pre-computed by scheduler)
     */
    public void sendContractExpiryAlert(
            String toEmail,
            String username,
            String documentTitle,
            Integer documentId,
            LocalDate endDate,
            long daysLeft) {

        try {
            String urgency = daysLeft <= 7 ? "URGENT: " : "";
            String subject = urgency + "Contract Expiring in " + daysLeft
                    + " day" + (daysLeft == 1 ? "" : "s") + " - Document Depository";

            String body =
                "Hello " + username + ",\n\n" +
                "This is an automated reminder that the following contract document " +
                "is expiring soon:\n\n" +
                "  Document : " + documentTitle + "\n" +
                "  Expires  : " + endDate.format(DATE_FMT) + "\n" +
                "  Days Left: " + daysLeft + " day" + (daysLeft == 1 ? "" : "s") + "\n\n" +
                "Please review the document and take any necessary renewal action.\n\n" +
                "You can view the document here:\n" +
                "  /myfiles (Document ID: " + documentId + ")\n\n" +
                "─────────────────────────────────────────\n" +
                "This is an automated message from the Document Depository system.\n" +
                "Please do not reply to this email.\n\n" +
                "Best regards,\n" +
                "Document Depository Team";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.log(Level.INFO,
                "Contract expiry alert sent to " + toEmail +
                " for document [" + documentId + "] '" + documentTitle +
                "' (" + daysLeft + " days left)");

        } catch (Exception e) {
            // Never let an email failure crash the scheduler
            log.log(Level.WARNING,
                "Failed to send contract expiry alert to " + toEmail +
                " for document " + documentId + ": " + e.getMessage());
        }
    }
}