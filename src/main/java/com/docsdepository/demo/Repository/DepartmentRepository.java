package com.docsdepository.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docsdepository.demo.Entity.Department;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * Get departments belonging to a specific office.
     * Used for cascading office → department dropdown.
     */
    @Query("SELECT d FROM Department d WHERE d.office.officeId = :officeId")
    List<Department> findByOfficeId(@Param("officeId") Integer officeId);
}