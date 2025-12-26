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
public interface UsersRepository extends JpaRepository<Users, Integer> {  // Changed from Long to Integer
    Optional<Users> findByUsername(String username);
    long countByLastLoginAfter(LocalDateTime time);
    List<Users> findTop20ByOrderByLastLoginDesc();

    @Query("""
    SELECT u
    FROM Users u
    JOIN FETCH u.office o
    JOIN FETCH o.area
    JOIN FETCH o.department
    WHERE u.userId = :userId
    """)
    Users findByIdWithOfficeHierarchy(@Param("userId") Integer userId);
}