package com.docsdepository.demo.Repository;

import com.docsdepository.demo.Entity.SearchHistory;
import com.docsdepository.demo.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Integer> {

    List<SearchHistory> findTop10ByUserOrderBySearchedAtDesc(Users user);

    @Query("SELECT DISTINCT s.keyword FROM SearchHistory s WHERE s.user = :user ORDER BY s.searchedAt DESC")
    List<String> findRecentUniqueKeywordsByUser(@Param("user") Users user);

    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedKeywordsGlobal();

    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "WHERE s.user = :user GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedKeywordsByUser(@Param("user") Users user);

    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "WHERE s.searchedAt >= :since GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findTrendingKeywords(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT s.keyword) FROM SearchHistory s WHERE s.user = :user")
    Long countUniqueSearchesByUser(@Param("user") Users user);

    @Query("SELECT COUNT(s) FROM SearchHistory s WHERE s.searchedAt >= :startOfDay")
    Long countSearchesToday(@Param("startOfDay") LocalDateTime startOfDay);
}