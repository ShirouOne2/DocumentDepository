package com.docsdepository.demo.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.docsdepository.demo.Entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findByUsername(String username);
    long countByLastLoginAfter(LocalDateTime time);
    List<Users> findTop20ByOrderByLastLoginDesc();

    /**
     * ✅ FIXED: Removed o.department (no longer exists on Office)
     * Department is now directly on Users entity
     */
    @Query("""
    SELECT u
    FROM Users u
    JOIN FETCH u.office o
    JOIN FETCH o.area
    LEFT JOIN FETCH u.department
    WHERE u.userId = :userId
    """)
    Users findByIdWithOfficeHierarchy(@Param("userId") Integer userId);

    Optional<Users> findByUsernameAndEmail(String username, String email);
    Optional<Users> findByEmail(String email);
    
    @Query("SELECT u FROM Users u WHERE u.role.name = 'ADMIN'")
    List<Users> findAllAdmins();

    /**
     * Load all users with role, office, area, and department eagerly fetched.
     * Used by NotificationService to avoid LazyInitializationException
     * when checking viewer group permissions outside a persistent context.
     */
    @Query("""
    SELECT DISTINCT u
    FROM Users u
    JOIN FETCH u.role
    JOIN FETCH u.office o
    JOIN FETCH o.area
    LEFT JOIN FETCH u.department
    """)
    List<Users> findAllWithDetails();

    /**
     * All active users belonging to a specific office.
     * Used by ContractExpiryScheduler for Office/Area viewer group alerts.
     */
    @Query("""
    SELECT DISTINCT u FROM Users u
    JOIN FETCH u.office o
    JOIN FETCH o.area
    LEFT JOIN FETCH u.department
    WHERE o.officeId = :officeId
      AND u.isActive = true
    """)
    List<Users> findActiveUsersByOfficeId(@Param("officeId") Integer officeId);

    /**
     * All active users belonging to a specific department.
     * Used by ContractExpiryScheduler for Department viewer group alerts.
     */
    @Query("""
    SELECT DISTINCT u FROM Users u
    JOIN FETCH u.office o
    JOIN FETCH o.area
    LEFT JOIN FETCH u.department d
    WHERE d.id = :departmentId
      AND u.isActive = true
    """)
    List<Users> findActiveUsersByDepartmentId(@Param("departmentId") Integer departmentId);
}