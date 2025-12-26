package com.docsdepository.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @Column(name = "search_date", nullable = false)
    private LocalDateTime searchDate;

    @Column(name = "search_type") // "myfiles", "shared", "all"
    private String searchType;

    @Column(name = "results_count") // Optional: how many results were found
    private Integer resultsCount;

    // Constructors
    public SearchHistory() {
    }

    public SearchHistory(Users user, String keyword, String searchType, Integer resultsCount) {
        this.user = user;
        this.keyword = keyword;
        this.searchType = searchType;
        this.resultsCount = resultsCount;
        this.searchDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LocalDateTime getSearchDate() {
        return searchDate;
    }

    public void setSearchDate(LocalDateTime searchDate) {
        this.searchDate = searchDate;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public Integer getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(Integer resultsCount) {
        this.resultsCount = resultsCount;
    }
}