package com.docsdepository.demo.Config;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

@Component
@WebListener
public class ActiveSessionCounter implements HttpSessionListener {

    private static final ConcurrentHashMap<String, Integer> activeSessions = new ConcurrentHashMap<>();
    private static final KeySetView<String, Boolean> processedSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        
        // Prevent duplicate processing
        if (processedSessions.contains(sessionId)) {
            System.out.println("⚠️ Duplicate session creation ignored: " + sessionId);
            return;
        }
        
        processedSessions.add(sessionId);
        System.out.println("✅ Session Created - Session ID: " + sessionId + " | Total Sessions: " + processedSessions.size());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        Integer userId = (Integer) se.getSession().getAttribute("userId");
        
        activeSessions.remove(sessionId);
        processedSessions.remove(sessionId);
        
        if (userId != null) {
            System.out.println("❌ Session Destroyed for User ID: " + userId + " | Session: " + sessionId);
        } else {
            System.out.println("❌ Session Destroyed (no user) | Session: " + sessionId);
        }
        
        System.out.println("📊 Total Sessions: " + processedSessions.size() + " | Logged-in Users: " + getLoggedInUserCount());
    }

    public static int getActiveSessions() {
        return processedSessions.size();
    }
    
    public static int getLoggedInUserCount() {
        return (int) activeSessions.values().stream().filter(userId -> userId != null).distinct().count();
    }
    
    public static void registerUserSession(String sessionId, Integer userId) {
        activeSessions.put(sessionId, userId);
        System.out.println("👤 User " + userId + " logged in | Session: " + sessionId + " | Logged-in Users: " + getLoggedInUserCount());
    }
}