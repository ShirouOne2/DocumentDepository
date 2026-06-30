package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Integer> {
    
    /**
     * Find offices by area
     */
    @Query("SELECT o FROM Office o WHERE o.area.id = :areaId")
    List<Office> findByAreaId(@Param("areaId") Integer areaId);
}