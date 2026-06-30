package com.docsdepository.demo.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SessionController {

    /**
     * Active keep-alive: touching the session resets its server-side timeout.
     * Only call this when the user is actively doing something (e.g. they clicked
     * "Stay logged in" on the warning dialog).
     */
    @GetMapping("/session-check")
    @ResponseBody
    public ResponseEntity<Void> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Passive peek: reports whether the session is still alive WITHOUT
     * resetting its server-side inactivity timeout.
     *
     * How it works: we calculate how many seconds remain and pin
     * maxInactiveInterval to that value so this request doesn't extend it.
     * The next real user request will restore the full timeout naturally
     * because Spring Boot reconfigures the session on every non-peek request.
     */
    @GetMapping("/session-peek")
    @ResponseBody
    public ResponseEntity<Void> peekSession(HttpServletRequest request,
                                             HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Pin remaining time so this request doesn't extend the session
        long lastAccessedMs = session.getLastAccessedTime();
        int  maxInactiveSec = session.getMaxInactiveInterval();
        long elapsedSec     = (System.currentTimeMillis() - lastAccessedMs) / 1000;
        int  remainingSec   = (int) Math.max(1, maxInactiveSec - elapsedSec);
        session.setMaxInactiveInterval(remainingSec);

        response.setHeader("Cache-Control", "no-store");
        return ResponseEntity.ok().build();
    }
}