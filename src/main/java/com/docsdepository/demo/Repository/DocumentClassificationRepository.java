package com.docsdepository.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docsdepository.demo.Entity.DocumentClassification;

@Repository
public interface DocumentClassificationRepository extends JpaRepository<DocumentClassification,Integer> {

}
