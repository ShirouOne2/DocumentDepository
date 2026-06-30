package com.docsdepository.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docsdepository.demo.Entity.Area;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

}
