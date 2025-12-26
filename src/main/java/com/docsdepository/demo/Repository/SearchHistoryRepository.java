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

    // Get user's recent searches (last 10)
    List<SearchHistory> findTop10ByUserOrderBySearchDateDesc(Users user);

    // Get user's recent unique searches
    @Query("SELECT DISTINCT s.keyword FROM SearchHistory s WHERE s.user = :user ORDER BY s.searchDate DESC")
    List<String> findRecentUniqueKeywordsByUser(@Param("user") Users user);

    // Most searched keywords globally
    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedKeywordsGlobal();

    // Most searched keywords by user
    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "WHERE s.user = :user GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedKeywordsByUser(@Param("user") Users user);

    // Trending searches (last 7 days)
    @Query("SELECT s.keyword, COUNT(s) as searchCount FROM SearchHistory s " +
           "WHERE s.searchDate >= :since GROUP BY s.keyword ORDER BY searchCount DESC")
    List<Object[]> findTrendingKeywords(@Param("since") LocalDateTime since);

    // Search statistics for dashboard
    @Query("SELECT COUNT(DISTINCT s.keyword) FROM SearchHistory s WHERE s.user = :user")
    Long countUniqueSearchesByUser(@Param("user") Users user);

    // Total searches today
    @Query("SELECT COUNT(s) FROM SearchHistory s WHERE s.searchDate >= :startOfDay")
    Long countSearchesToday(@Param("startOfDay") LocalDateTime startOfDay);
}