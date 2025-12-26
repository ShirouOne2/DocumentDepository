package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.SearchHistory;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchAnalyticsService {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void recordSearch(Users user, String keyword, String searchType, Integer resultsCount) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        SearchHistory history = new SearchHistory(
            user,
            keyword.trim().toLowerCase(), // Normalize to lowercase
            searchType,
            resultsCount
        );
        
        searchHistoryRepository.save(history);
    }

    public List<String> getRecentSearches(Users user, int limit) {
        return searchHistoryRepository.findTop10ByUserOrderBySearchDateDesc(user)
            .stream()
            .map(SearchHistory::getKeyword)
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<KeywordCount> getMostSearchedByUser(Users user) {
        return searchHistoryRepository.findMostSearchedKeywordsByUser(user)
            .stream()
            .map(result -> new KeywordCount((String) result[0], (Long) result[1]))
            .limit(10)
            .collect(Collectors.toList());
    }

    public List<KeywordCount> getMostSearchedGlobal() {
        return searchHistoryRepository.findMostSearchedKeywordsGlobal()
            .stream()
            .map(result -> new KeywordCount((String) result[0], (Long) result[1]))
            .limit(10)
            .collect(Collectors.toList());
    }

    public List<KeywordCount> getTrendingSearches() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return searchHistoryRepository.findTrendingKeywords(sevenDaysAgo)
            .stream()
            .map(result -> new KeywordCount((String) result[0], (Long) result[1]))
            .limit(10)
            .collect(Collectors.toList());
    }

    // Inner class for returning keyword counts
    public static class KeywordCount {
        private String keyword;
        private Long count;

        public KeywordCount(String keyword, Long count) {
            this.keyword = keyword;
            this.count = count;
        }

        public String getKeyword() {
            return keyword;
        }

        public Long getCount() {
            return count;
        }
    }
}