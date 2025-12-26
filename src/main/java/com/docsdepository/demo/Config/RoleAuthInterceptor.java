package com.docsdepository.demo.Config;

import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component  // ← ADD THIS!
public class RoleAuthInterceptor implements HandlerInterceptor {

    private final UsersRepository usersRepository;

    // Constructor injection instead of @Autowired
    public RoleAuthInterceptor(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Allow all static resources & public pages
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

        // Admin-only routes (role ID 4 = ADMIN)
        if (
            uri.startsWith("/admin") ||
            uri.startsWith("/users") ||
            uri.startsWith("/system")
        ) {
            if (user.getRole() == null || user.getRole().getId() != 4) {
                response.sendRedirect("/access-denied");
                return false;
            }
        }

        return true;
    }
}