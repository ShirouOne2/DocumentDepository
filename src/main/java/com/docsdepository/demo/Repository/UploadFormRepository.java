package com.docsdepository.demo.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.docsdepository.demo.Entity.UploadForm;

@Repository
public interface UploadFormRepository extends JpaRepository<UploadForm, Long> {
    
    
}