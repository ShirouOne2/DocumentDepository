package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    Optional<Tag> findByTagName(String tagName);
    
    @Query("SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Tag> searchTags(@Param("query") String query);
    
    @Query("SELECT t FROM Tag t ORDER BY SIZE(t.documents) DESC")
    List<Tag> findMostUsedTags();
    
    @Query("SELECT t FROM Tag t WHERE SIZE(t.documents) > 0 ORDER BY t.tagName ASC")
    List<Tag> findAllActiveTags();
}