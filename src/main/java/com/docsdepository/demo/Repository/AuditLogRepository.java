package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(Integer userId);
    List<AuditLog> findTop100ByOrderByTimestampDesc();

    /**
     * Paginated, filtered query used by AuditController.
     * All filter params are optional — passing null skips that condition.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:action   IS NULL OR a.action = :action)
          AND (:startDt  IS NULL OR a.timestamp >= :startDt)
          AND (:endDt    IS NULL OR a.timestamp <= :endDt)
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findFiltered(
        @Param("username") String username,
        @Param("action")   String action,
        @Param("startDt")  LocalDateTime startDt,
        @Param("endDt")    LocalDateTime endDt,
        Pageable pageable
    );

    /** Count distinct users who have a log entry on or after the given time. */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM AuditLog a WHERE a.timestamp >= :since")
    long countDistinctUsersSince(@Param("since") LocalDateTime since);

    /** Total log entries that match the same filter (used for stats card). */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:action   IS NULL OR a.action = :action)
          AND (:startDt  IS NULL OR a.timestamp >= :startDt)
          AND (:endDt    IS NULL OR a.timestamp <= :endDt)
    """)
    long countFiltered(
        @Param("username") String username,
        @Param("action")   String action,
        @Param("startDt")  LocalDateTime startDt,
        @Param("endDt")    LocalDateTime endDt
    );
}