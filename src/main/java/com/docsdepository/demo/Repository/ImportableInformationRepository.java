package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.ImportableInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportableInformationRepository extends JpaRepository<ImportableInformation, Long> {

    // Custom method for searching (matches your Thymeleaf search action)
    List<ImportableInformation> findByTitleContainingIgnoreCase(String title);
}