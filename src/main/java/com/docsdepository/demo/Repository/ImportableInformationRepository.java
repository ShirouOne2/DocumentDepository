package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.ImportableInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ImportableInformationRepository extends JpaRepository<ImportableInformation, Long> {

    // Custom method for searching (matches your Thymeleaf search action)
    List<ImportableInformation> findByTitleContainingIgnoreCase(String title);
    @Query("SELECT COUNT(i) FROM ImportableInformation i WHERE DATE(i.uploadDate) = :today")
    long countUploadsToday(@Param("today") java.time.LocalDate today);
}