package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.Notification;
import com.docsdepository.demo.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    // Count unread notifications for a user
    Long countByUserAndIsReadFalse(Users user);
    
    // Get recent notifications (read + unread)
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(Users user);
    
    // Get all notifications for a user (paginated in service)
    List<Notification> findByUserOrderByCreatedAtDesc(Users user);
    
    // Get only unread notifications
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(Users user);
    
    // Mark notification as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :id AND n.user = :user")
    void markAsRead(@Param("id") Integer notificationId, @Param("user") Users user);
    
    // Mark all as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") Users user);
    
    // Delete old read notifications (cleanup - optional)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("user") Users user, @Param("cutoffDate") LocalDateTime cutoffDate);
}