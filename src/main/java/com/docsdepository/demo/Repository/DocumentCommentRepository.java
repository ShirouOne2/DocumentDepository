package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Integer> {
    
    List<DocumentComment> findByDocumentDocumentIdOrderByCreatedDateDesc(Integer documentId);
    
    Long countByDocumentDocumentId(Integer documentId);
    
    void deleteByDocumentDocumentId(Integer documentId);
}