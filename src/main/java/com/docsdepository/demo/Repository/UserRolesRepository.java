package com.docsdepository.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docsdepository.demo.Entity.UserRoles;

@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, Integer> {
}