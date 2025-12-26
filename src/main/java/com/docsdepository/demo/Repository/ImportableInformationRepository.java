package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportableInformationRepository extends JpaRepository<ImportableInformation, Integer> {

    // ========== BASIC QUERIES ==========
    
    // Custom method for searching (matches your Thymeleaf search action)
    List<ImportableInformation> findByTitleContainingIgnoreCase(String title);

    // Count uploads today
    @Query("""
        SELECT COUNT(i)
        FROM ImportableInformation i
        WHERE i.uploadDate >= :start
        AND i.uploadDate < :end
    """)
    long countUploadsToday(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    // ========== USER DOCUMENTS ==========
    
    // Find all documents by uploader
    List<ImportableInformation> findByUploadedBy(Users user);

    // Find active (non-archived) documents by uploader
    List<ImportableInformation> findByUploadedByAndIsArchivedFalse(Users user);
    
    // Search within current user's documents only
    List<ImportableInformation> findByUploadedByAndTitleContainingIgnoreCase(Users user, String title);

    // Count user's documents
    long countByUploadedBy(Users user);

    // ========== SHARED FILES ==========
    
    /**
     * Find all documents that the current user's office is allowed to view
     * based on office_viewer mappings. Excludes documents uploaded by the current user.
     * Excludes archived documents.
     */
    @Query("""
    SELECT DISTINCT i
    FROM ImportableInformation i
    JOIN i.uploadedBy u
    WHERE u.userId <> :userId
    AND (i.isArchived = false OR i.isArchived IS NULL)
    AND (
        i.intendedViewerGroup.id = 1

        OR (
            i.intendedViewerGroup.id = 2
            AND u.office.area.id = :areaId
        )

        OR (
            i.intendedViewerGroup.id = 3
            AND u.office.officeId = :officeId
        )

        OR (
            i.intendedViewerGroup.id = 4
            AND u.office.officeId = :officeId
            AND u.office.area.id = :areaId
            AND u.office.department.id = :departmentId
        )
    )
    ORDER BY i.dateCreated DESC
    """)
    List<ImportableInformation> findSharedFiles(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId
    );


    // ========== ADVANCED SEARCH ==========
    
    // Search shared files with filters
    @Query("""
    SELECT DISTINCT i
    FROM ImportableInformation i
    JOIN i.uploadedBy u
    WHERE u.userId <> :userId
    AND (i.isArchived = false OR i.isArchived IS NULL)

    AND (
        i.intendedViewerGroup.id = 1
        OR (i.intendedViewerGroup.id = 2 AND u.office.area.id = :areaId)
        OR (i.intendedViewerGroup.id = 3 AND u.office.officeId = :officeId)
        OR (i.intendedViewerGroup.id = 4
            AND u.office.officeId = :officeId
            AND u.office.area.id = :areaId
            AND u.office.department.id = :departmentId)
    )

    AND (
        :title IS NULL OR
        LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%')) OR
        LOWER(i.description) LIKE LOWER(CONCAT('%', :title, '%'))
    )

    AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
    AND (:uploadedById IS NULL OR u.userId = :uploadedById)
    AND (:startDate IS NULL OR i.dateCreated >= :startDate)
    AND (:endDate IS NULL OR i.dateCreated <= :endDate)
    """)
    List<ImportableInformation> searchSharedFiles(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        @Param("title") String title,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );


    // Search user's own files with filters
    @Query("SELECT i FROM ImportableInformation i " +
        "WHERE i.uploadedBy.userId = :userId " +
        "AND (i.isArchived = false OR i.isArchived IS NULL) " +  // Exclude archived
        "AND (:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
        "     OR LOWER(i.description) LIKE LOWER(CONCAT('%', :title, '%'))) " +
        "AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId) " +
        "AND (:startDate IS NULL OR i.dateCreated >= :startDate) " +
        "AND (:endDate IS NULL OR i.dateCreated <= :endDate)")
    List<ImportableInformation> searchMyFiles(
            @Param("userId") Integer userId,
            @Param("title") String title,
            @Param("classificationId") Integer classificationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ========== DASHBOARD QUERIES ==========
    
    @EntityGraph(attributePaths = {"uploadedBy", "documentClassification"})
    List<ImportableInformation> findTop10ByOrderByUploadDateDesc();
    
    // Count by classification (for dashboard chart)
    long countByDocumentClassification(DocumentClassification classification);

    @Query("SELECT ii FROM ImportableInformation ii " +
           "LEFT JOIN FETCH ii.uploadedBy " +
           "LEFT JOIN FETCH ii.documentClassification " +
           "WHERE ii.isArchived = false " +
           "AND (:query IS NULL OR LOWER(ii.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "    OR LOWER(ii.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:classificationId IS NULL OR ii.documentClassification.id = :classificationId) " +
           "AND (:uploadedById IS NULL OR ii.uploadedBy.userId = :uploadedById) " +
           "AND (:startDate IS NULL OR ii.dateCreated >= :startDate) " +
           "AND (:endDate IS NULL OR ii.dateCreated <= :endDate) " +
           "ORDER BY ii.uploadDate DESC")
    List<ImportableInformation> searchAllDocuments(
            @Param("query") String query,
            @Param("classificationId") Integer classificationId,
            @Param("uploadedById") Integer uploadedById,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    List<ImportableInformation> findByIsArchivedFalse();
}