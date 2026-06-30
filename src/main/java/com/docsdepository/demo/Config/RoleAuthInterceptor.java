package com.docsdepository.demo.Config;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthInterceptor implements HandlerInterceptor {

    private final UsersRepository usersRepository;
    private final AuditService auditService;

    public RoleAuthInterceptor(UsersRepository usersRepository, AuditService auditService) {
        this.usersRepository = usersRepository;
        this.auditService = auditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Allow all static resources & public pages
        if (
            uri.equals("/Userlogin") ||
            uri.equals("/login") ||
            uri.equals("/logout") ||
            uri.equals("/session-timeout") ||
            uri.startsWith("/session-check") ||
            uri.startsWith("/session-peek") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/assets/")
        ) {
            return true;
        }

        // Check session for protected routes
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect("/Userlogin");
            return false;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect("/Userlogin");
            return false;
        }

        Users user = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (user == null) {
            session.invalidate();
            response.sendRedirect("/Userlogin");
            return false;
        }

        // 📝 LOG ONLY SPECIFIC ACTIONS
        if (shouldLogAction(uri)) {
            auditService.log(userId, user.getUsername(), method, uri, request);
        }

        // Admin-only routes
        if (
            uri.startsWith("/admin") ||
            uri.startsWith("/users") ||
            uri.startsWith("/system")
        ) {
            if (user.getRole() == null || 
                (user.getRole().getId() != 1 && !"ADMIN".equalsIgnoreCase(user.getRole().getName()))) {
                response.sendRedirect("/access-denied");
                return false;
            }
        }

        return true;
    }

    private boolean shouldLogAction(String uri) {
        return uri.startsWith("/documents/details/") ||
               uri.startsWith("/documents/view/") ||
               uri.startsWith("/documents/download/") ||
               uri.startsWith("/myfiles/search");
    }
}