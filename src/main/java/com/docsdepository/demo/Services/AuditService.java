package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.AuditLog;
import com.docsdepository.demo.Repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    // Original method - used by RoleAuthInterceptor for document actions
    @Async
    public void log(Integer userId, String username, String action, 
                    String endpoint, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction(action);
            log.setEndpoint(endpoint);
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }
    
    // ============================================
    // NEW METHODS FOR LOGIN/LOGOUT/TIMEOUT
    // ============================================
    
    @Async
    public void logLogin(Integer userId, String username, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction("LOGIN");
            log.setEndpoint("/login");
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Login audit logging failed: " + e.getMessage());
        }
    }
    
    @Async
    public void logLogout(Integer userId, String username, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction("LOGOUT");
            log.setEndpoint("/logout");
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Logout audit logging failed: " + e.getMessage());
        }
    }
    
    @Async
    public void logTimeout(Integer userId, String username, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction("TIMEOUT");
            log.setEndpoint("/session-timeout");
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Timeout audit logging failed: " + e.getMessage());
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip : request.getRemoteAddr();
    }
}