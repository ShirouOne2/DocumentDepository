package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.DocumentClassification;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ImportableInformationRepository extends JpaRepository<ImportableInformation, Integer> {

    List<ImportableInformation> findByTitleContainingIgnoreCase(String title);

    /**
     * Load a single document with all relations needed for permission checks.
     * Used by NotificationService.isNotificationValid to avoid LazyInitializationException.
     */
    @Query("""
        SELECT DISTINCT i FROM ImportableInformation i
        LEFT JOIN FETCH i.intendedViewerGroup
        LEFT JOIN FETCH i.uploadedBy u
        LEFT JOIN FETCH u.office o
        LEFT JOIN FETCH o.area
        LEFT JOIN FETCH u.department
        LEFT JOIN FETCH i.customViewerGroup cg
        LEFT JOIN FETCH cg.members
        LEFT JOIN FETCH i.targetArea
        LEFT JOIN FETCH i.targetOffice
        LEFT JOIN FETCH i.targetDepartment
        WHERE i.documentId = :docId
    """)
    Optional<ImportableInformation> findByIdWithDetails(@Param("docId") Integer docId);

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

    List<ImportableInformation> findByUploadedBy(Users user);
    List<ImportableInformation> findByUploadedByAndIsArchivedFalse(Users user);
    long countByUploadedBy(Users user);
    
    @EntityGraph(attributePaths = {"uploadedBy", "documentClassification"})
    List<ImportableInformation> findTop10ByOrderByUploadDateDesc();
    
    long countByDocumentClassification(DocumentClassification classification);
    List<ImportableInformation> findByIsArchivedFalse();
    List<ImportableInformation> findByUploadedBy_UserId(Integer userId);
    Page<ImportableInformation> findByUploadedByAndIsArchivedFalse(Users user, Pageable pageable);
    Page<ImportableInformation> findByIsArchivedFalse(Pageable pageable);

    // ========================================
    // MULTI-TERM SEARCH - MY FILES
    // AND logic: document must match ALL provided terms.
    // FIX: COALESCE(:termN, '%') makes a NULL term match everything,
    //      so filtering by uploadedById/classification alone still returns results.
    // ========================================
    @Query("""
        SELECT DISTINCT d FROM ImportableInformation d 
        WHERE d.uploadedBy.userId = :userId 
        AND d.isArchived = false 
        AND (LOWER(d.title) LIKE COALESCE(:term1, '%')
                OR LOWER(d.description) LIKE COALESCE(:term1, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term1, '%')))
        AND (LOWER(d.title) LIKE COALESCE(:term2, '%')
                OR LOWER(d.description) LIKE COALESCE(:term2, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term2, '%')))
        AND (LOWER(d.title) LIKE COALESCE(:term3, '%')
                OR LOWER(d.description) LIKE COALESCE(:term3, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term3, '%')))
        AND (:classificationId IS NULL OR d.documentClassification.id = :classificationId) 
        AND (:startDate IS NULL OR d.documentDate >= :startDate) 
        AND (:endDate IS NULL OR d.documentDate <= :endDate)
    """)
    Page<ImportableInformation> searchMyFilesMultiTerm(
        @Param("userId") Integer userId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("""
        SELECT DISTINCT d FROM ImportableInformation d 
        WHERE d.uploadedBy.userId = :userId 
        AND d.isArchived = false 
        AND (LOWER(d.title) LIKE COALESCE(:term1, '%')
                OR LOWER(d.description) LIKE COALESCE(:term1, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term1, '%')))
        AND (LOWER(d.title) LIKE COALESCE(:term2, '%')
                OR LOWER(d.description) LIKE COALESCE(:term2, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term2, '%')))
        AND (LOWER(d.title) LIKE COALESCE(:term3, '%')
                OR LOWER(d.description) LIKE COALESCE(:term3, '%')
                OR EXISTS (SELECT 1 FROM d.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term3, '%')))
        AND (:classificationId IS NULL OR d.documentClassification.id = :classificationId) 
        AND (:startDate IS NULL OR d.documentDate >= :startDate) 
        AND (:endDate IS NULL OR d.documentDate <= :endDate)
        ORDER BY d.uploadDate DESC
    """)
    List<ImportableInformation> searchMyFilesMultiTerm(
        @Param("userId") Integer userId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );


    // ========================================
    // MULTI-TERM SEARCH - SHARED FILES (non-custom-group)
    // OR logic: document matches if it contains ANY of the provided terms.
    // ========================================
    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u

        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)

        AND (
            i.intendedViewerGroup.id = 1
            OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
            OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
            OR (i.intendedViewerGroup.id = 4
                AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
            )
        )

        AND (
            (:term1 IS NULL AND :term2 IS NULL AND :term3 IS NULL)
            OR (:term1 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term1, '%')))
            ))
            OR (:term2 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term2, '%')))
            ))
            OR (:term3 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term3, '%')))
            ))
        )

        AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
        AND (:uploadedById IS NULL OR u.userId = :uploadedById)
        AND (:startDate IS NULL OR i.documentDate >= :startDate)
        AND (:endDate IS NULL OR i.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(i.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(i.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                i.targetDepartment IS NOT NULL AND i.targetDepartment.id = :filterDepartmentId
            ))

        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> searchSharedFilesMultiTerm(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        Pageable pageable
    );

    // ========================================
    // MULTI-TERM SEARCH - ADMIN ALL DOCS (no extra filters)
    // OR logic: document matches if it contains ANY of the provided terms.
    // ========================================
    @Query("""
        SELECT DISTINCT ii FROM ImportableInformation ii 
        LEFT JOIN ii.uploadedBy 
        LEFT JOIN ii.documentClassification 
        WHERE ii.isArchived = false 
        AND (
            (:term1 IS NULL AND :term2 IS NULL AND :term3 IS NULL)
            OR (:term1 IS NOT NULL AND (
                LOWER(ii.title) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR LOWER(ii.description) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term1, '%')))
            ))
            OR (:term2 IS NOT NULL AND (
                LOWER(ii.title) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR LOWER(ii.description) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term2, '%')))
            ))
            OR (:term3 IS NOT NULL AND (
                LOWER(ii.title) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR LOWER(ii.description) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term3, '%')))
            ))
        )
        AND (:classificationId IS NULL OR ii.documentClassification.id = :classificationId) 
        AND (:uploadedById IS NULL OR ii.uploadedBy.userId = :uploadedById) 
        AND (:startDate IS NULL OR ii.documentDate >= :startDate) 
        AND (:endDate IS NULL OR ii.documentDate <= :endDate) 
        ORDER BY ii.uploadDate DESC
    """)
    Page<ImportableInformation> searchAllDocumentsMultiTerm(
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // ========================================
    // Keep non-paginated versions for backward compatibility
    // ========================================
    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            i.intendedViewerGroup.id = 1
            OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
            OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
            OR (i.intendedViewerGroup.id = 4
                AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
            )
        )
        ORDER BY i.uploadDate DESC
    """)
    List<ImportableInformation> findSharedFiles(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId
    );

    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            i.intendedViewerGroup.id = 1
            OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
            OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
            OR (i.intendedViewerGroup.id = 4
                AND :officeId IS NOT NULL
                AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
            )
        )
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> findSharedFiles(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        Pageable pageable
    );

    // ============================================
    // SHARED FILES WITH CUSTOM VIEWER GROUPS
    // ============================================

    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            (i.customViewerGroup IS NULL AND (
                i.intendedViewerGroup.id = 1
                OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                    AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
                OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
                OR (i.intendedViewerGroup.id = 4
                    AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                    AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
                )
            ))
            OR
            (i.customViewerGroup IS NOT NULL 
            AND i.intendedViewerGroup.id = 5
            AND EXISTS (
                SELECT 1 FROM i.customViewerGroup.members m 
                WHERE m.userId = :userId
            ))
        )
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> findSharedFilesWithCustomGroups(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        Pageable pageable
    );

    // FIX: COALESCE(:termN, '%') makes a NULL term match everything.
    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            (i.customViewerGroup IS NULL AND (
                i.intendedViewerGroup.id = 1
                OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                    AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
                OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
                OR (i.intendedViewerGroup.id = 4
                    AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                    AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
                )
            ))
            OR
            (i.customViewerGroup IS NOT NULL
            AND i.intendedViewerGroup.id = 5
            AND EXISTS (
                SELECT 1 FROM i.customViewerGroup.members m
                WHERE m.userId = :userId
            ))
        )
        AND (LOWER(i.title) LIKE COALESCE(:term1, '%')
                OR LOWER(i.description) LIKE COALESCE(:term1, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term1, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term1, '%')))
        AND (LOWER(i.title) LIKE COALESCE(:term2, '%')
                OR LOWER(i.description) LIKE COALESCE(:term2, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term2, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term2, '%')))
        AND (LOWER(i.title) LIKE COALESCE(:term3, '%')
                OR LOWER(i.description) LIKE COALESCE(:term3, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term3, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term3, '%')))
        AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
        AND (:uploadedById IS NULL OR u.userId = :uploadedById)
        AND (:startDate IS NULL OR i.documentDate >= :startDate)
        AND (:endDate IS NULL OR i.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(i.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(i.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                i.targetDepartment IS NOT NULL AND i.targetDepartment.id = :filterDepartmentId
            ))
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> searchSharedFilesWithCustomGroupsMultiTerm(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        Pageable pageable
    );

    @Query("""
        SELECT d FROM ImportableInformation d 
        WHERE d.documentId = :documentId 
        OR d.replacesDocument.documentId = :documentId 
        OR d.replacedByDocument.documentId = :documentId
        ORDER BY d.versionNumber DESC
    """)
    List<ImportableInformation> findAllVersions(@Param("documentId") Integer documentId);

    @Query("""
        SELECT d FROM ImportableInformation d 
        WHERE (
            d.documentId = :documentId 
            OR (d.replacesDocument IS NOT NULL 
                AND d.replacesDocument.documentId = :documentId)
        )
        AND (d.isArchived = false OR d.isArchived IS NULL)
    """)
    Optional<ImportableInformation> findCurrentVersion(
            @Param("documentId") Integer documentId
    );

    @Query("""
        SELECT d FROM ImportableInformation d 
        WHERE d.replacedByDocument.documentId = :currentDocId 
        AND d.isArchived = true
        ORDER BY d.versionNumber DESC
    """)
    List<ImportableInformation> findVersionHistory(@Param("currentDocId") Integer currentDocId);

    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            (i.customViewerGroup IS NULL AND (
                i.intendedViewerGroup.id = 1
                OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL AND i.targetArea.id = :areaId)
                OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL AND i.targetOffice.officeId = :officeId)
                OR (i.intendedViewerGroup.id = 4
                    AND :officeId IS NOT NULL
                    AND i.targetOffice.officeId = :officeId
                    AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
                )
            ))
            OR
            (i.customViewerGroup IS NOT NULL 
            AND i.intendedViewerGroup.id = 5
            AND EXISTS (
                SELECT 1 FROM i.customViewerGroup.members m 
                WHERE m.userId = :userId
            ))
        )
        AND (
            (:term1 IS NULL AND :term2 IS NULL AND :term3 IS NULL)
            OR (:term1 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term1, '%')))
            ))
            OR (:term2 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term2, '%')))
            ))
            OR (:term3 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term3, '%')))
            ))
        )
        AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
        AND (:uploadedById IS NULL OR u.userId = :uploadedById)
        AND (:startDate IS NULL OR i.documentDate >= :startDate)
        AND (:endDate IS NULL OR i.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(i.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(i.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                i.targetDepartment IS NOT NULL AND i.targetDepartment.id = :filterDepartmentId
            ))
        AND (:viewerGroupName IS NULL OR i.intendedViewerGroup.name = :viewerGroupName)
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> searchSharedFilesWithViewerGroupFilter(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        @Param("viewerGroupName") String viewerGroupName,
        Pageable pageable
    );

    // FIX: JOIN i.customViewerGroup cvg + cvg.groupId prevents implicit inner join issues.
    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        JOIN i.customViewerGroup cvg
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND i.intendedViewerGroup.id = 5
        AND cvg.groupId = :customGroupId
        AND EXISTS (
            SELECT 1 FROM i.customViewerGroup.members m 
            WHERE m.userId = :userId
        )
        AND (
            (:term1 IS NULL AND :term2 IS NULL AND :term3 IS NULL)
            OR (:term1 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term1, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term1, '%')))
            ))
            OR (:term2 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term2, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term2, '%')))
            ))
            OR (:term3 IS NOT NULL AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term3, '%'))
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :term3, '%')))
            ))
        )
        AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
        AND (:uploadedById IS NULL OR u.userId = :uploadedById)
        AND (:startDate IS NULL OR i.documentDate >= :startDate)
        AND (:endDate IS NULL OR i.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(i.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(i.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                i.targetDepartment IS NOT NULL AND i.targetDepartment.id = :filterDepartmentId
            ))
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> searchSharedFilesByCustomGroup(
        @Param("userId") Integer userId,
        @Param("customGroupId") Integer customGroupId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        Pageable pageable
    );

    // FIX 1: COALESCE(:termN, '%') makes a NULL term match everything.
    // FIX 2: LEFT JOIN cvg alias prevents implicit inner join on customViewerGroup.
    // FIX 3: Department filter uses EXISTS subquery.
    @Query("""
        SELECT DISTINCT i
        FROM ImportableInformation i
        JOIN i.uploadedBy u
        LEFT JOIN i.customViewerGroup cvg
        WHERE u.userId <> :userId
        AND (i.isArchived = false OR i.isArchived IS NULL)
        AND (
            (i.customViewerGroup IS NULL AND (
                i.intendedViewerGroup.id = 1
                OR (i.intendedViewerGroup.id = 2 AND :areaId IS NOT NULL
                    AND COALESCE(i.targetArea.id, u.office.area.id) = :areaId)
                OR (i.intendedViewerGroup.id = 3 AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId)
                OR (i.intendedViewerGroup.id = 4
                    AND :officeId IS NOT NULL
                    AND COALESCE(i.targetOffice.officeId, u.office.officeId) = :officeId
                    AND (i.targetDepartment IS NULL OR (:departmentId IS NOT NULL AND i.targetDepartment.id = :departmentId))
                )
            ))
            OR
            (i.customViewerGroup IS NOT NULL
            AND i.intendedViewerGroup.id = 5
            AND EXISTS (
                SELECT 1 FROM i.customViewerGroup.members m
                WHERE m.userId = :userId
            ))
        )
        AND (LOWER(i.title) LIKE COALESCE(:term1, '%')
                OR LOWER(i.description) LIKE COALESCE(:term1, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term1, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term1, '%')))
        AND (LOWER(i.title) LIKE COALESCE(:term2, '%')
                OR LOWER(i.description) LIKE COALESCE(:term2, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term2, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term2, '%')))
        AND (LOWER(i.title) LIKE COALESCE(:term3, '%')
                OR LOWER(i.description) LIKE COALESCE(:term3, '%')
                OR LOWER(i.dtsNumber) LIKE COALESCE(:term3, '%')
                OR EXISTS (SELECT 1 FROM i.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term3, '%')))
        AND (:classificationId IS NULL OR i.documentClassification.id = :classificationId)
        AND (:uploadedById IS NULL OR u.userId = :uploadedById)
        AND (:startDate IS NULL OR i.documentDate >= :startDate)
        AND (:endDate IS NULL OR i.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(i.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(i.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                i.targetDepartment IS NOT NULL AND i.targetDepartment.id = :filterDepartmentId
            ))
        AND (:filterCustomGroupId IS NULL OR cvg.groupId = :filterCustomGroupId)
        ORDER BY i.uploadDate DESC
    """)
    Page<ImportableInformation> searchSharedFilesWithCustomGroupsMultiTermAndFilter(
        @Param("userId") Integer userId,
        @Param("officeId") Integer officeId,
        @Param("areaId") Integer areaId,
        @Param("departmentId") Integer departmentId,
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        @Param("filterCustomGroupId") Integer filterCustomGroupId,
        Pageable pageable
    );

    // FIX 1: COALESCE(:termN, '%') makes a NULL term match everything.
    // FIX 2: LEFT JOIN cvg alias prevents implicit inner join.
    // FIX 3: Department filter uses EXISTS subquery.
    @Query("""
        SELECT DISTINCT ii FROM ImportableInformation ii 
        LEFT JOIN ii.uploadedBy u
        LEFT JOIN ii.documentClassification
        LEFT JOIN ii.customViewerGroup cvg
        WHERE ii.isArchived = false 
        AND (LOWER(ii.title) LIKE COALESCE(:term1, '%')
                OR LOWER(ii.description) LIKE COALESCE(:term1, '%')
                OR LOWER(ii.dtsNumber) LIKE COALESCE(:term1, '%')
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term1, '%')))
        AND (LOWER(ii.title) LIKE COALESCE(:term2, '%')
                OR LOWER(ii.description) LIKE COALESCE(:term2, '%')
                OR LOWER(ii.dtsNumber) LIKE COALESCE(:term2, '%')
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term2, '%')))
        AND (LOWER(ii.title) LIKE COALESCE(:term3, '%')
                OR LOWER(ii.description) LIKE COALESCE(:term3, '%')
                OR LOWER(ii.dtsNumber) LIKE COALESCE(:term3, '%')
                OR EXISTS (SELECT 1 FROM ii.tags t WHERE LOWER(t.tagName) LIKE COALESCE(:term3, '%')))
        AND (:classificationId IS NULL OR ii.documentClassification.id = :classificationId) 
        AND (:uploadedById IS NULL OR u.userId = :uploadedById) 
        AND (:startDate IS NULL OR ii.documentDate >= :startDate) 
        AND (:endDate IS NULL OR ii.documentDate <= :endDate)
        AND (:filterAreaId IS NULL OR COALESCE(ii.targetArea.id, u.office.area.id) = :filterAreaId)
        AND (:filterOfficeId IS NULL OR COALESCE(ii.targetOffice.officeId, u.office.officeId) = :filterOfficeId)
        AND (:filterDepartmentId IS NULL OR (
                ii.targetDepartment IS NOT NULL AND ii.targetDepartment.id = :filterDepartmentId
            ))
        AND (:filterCustomGroupId IS NULL OR cvg.groupId = :filterCustomGroupId)
        ORDER BY ii.uploadDate DESC
    """)
    Page<ImportableInformation> searchAllDocumentsMultiTermWithFilters(
        @Param("term1") String term1,
        @Param("term2") String term2,
        @Param("term3") String term3,
        @Param("classificationId") Integer classificationId,
        @Param("uploadedById") Integer uploadedById,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("filterAreaId") Integer filterAreaId,
        @Param("filterOfficeId") Integer filterOfficeId,
        @Param("filterDepartmentId") Integer filterDepartmentId,
        @Param("filterCustomGroupId") Integer filterCustomGroupId,
        Pageable pageable
    );

    @Query("SELECT d FROM ImportableInformation d WHERE d.isArchived = true AND d.replacedByDocument IS NULL")
    List<ImportableInformation> findCurrentArchivedDocuments();

    /**
     * Fetch all active (non-archived) documents that have a contract end date set.
     * Used by ContractExpiryScheduler — uploadedBy is joined eagerly so the
     * scheduler can read email/username without lazy-load outside a transaction.
     */
    @Query("""
        SELECT d FROM ImportableInformation d
        JOIN FETCH d.uploadedBy
        WHERE d.isArchived = false
          AND d.contractEndDate IS NOT NULL
    """)
    List<ImportableInformation> findActiveContractsWithEndDate();
}