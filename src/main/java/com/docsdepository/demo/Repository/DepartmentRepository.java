package com.docsdepository.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docsdepository.demo.Entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
