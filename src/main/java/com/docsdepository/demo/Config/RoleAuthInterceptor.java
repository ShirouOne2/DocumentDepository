package com.docsdepository.demo.Config;

import com.docsdepository.demo.Entity.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class RoleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // ✅ ALLOW ALL STATIC RESOURCES & PUBLIC PAGES
        if (
            uri.equals("/Userlogin") ||
            uri.equals("/login") ||
            uri.equals("/logout") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/assets/") ||
            uri.endsWith(".css") ||
            uri.endsWith(".js") ||
            uri.endsWith(".png") ||
            uri.endsWith(".jpg") ||
            uri.endsWith(".ico")
        ) {
            return true;
        }

        // ✅ CHECK SESSION FOR PROTECTED ROUTES
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("/Userlogin");
            return false;
        }

        Users user = (Users) session.getAttribute("user");

        // ✅ ADMIN-ONLY ROUTES
        if (
            uri.startsWith("/admin") ||
            uri.startsWith("/users") ||
            uri.startsWith("/system")
        ) {
            if (!"ADMIN".equals(user.getRole())) {
                response.sendRedirect("/access-denied");
                return false;
            }
        }

        return true;
    }
}