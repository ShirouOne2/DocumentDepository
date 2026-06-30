package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.Tag;
import com.docsdepository.demo.Repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    /**
     * Parse comma-separated tags and return Tag entities
     * Creates new tags if they don't exist
     */
    @Transactional
    public Set<Tag> parseTags(String tagsInput) {
        if (tagsInput == null || tagsInput.trim().isEmpty()) {
            return new HashSet<>();
        }

        return java.util.Arrays.stream(tagsInput.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .map(String::toLowerCase)
            .limit(10) // Max 10 tags per document
            .map(this::getOrCreateTag)
            .collect(Collectors.toSet());
    }

    /**
     * Get existing tag or create new one
     */
    @Transactional
    public Tag getOrCreateTag(String tagName) {
        String normalizedName = tagName.toLowerCase().trim();
        
        return tagRepository.findByTagName(normalizedName)
            .orElseGet(() -> tagRepository.save(new Tag(normalizedName)));
    }

    /**
     * Search tags by query
     */
    public List<Tag> searchTags(String query) {
        return tagRepository.searchTags(query);
    }

    /**
     * Get most used tags (for autocomplete)
     */
    public List<Tag> getMostUsedTags() {
        return tagRepository.findMostUsedTags()
            .stream()
            .limit(20)
            .collect(Collectors.toList());
    }

    /**
     * Get all active tags (used in at least 1 document)
     */
    public List<Tag> getAllActiveTags() {
        return tagRepository.findAllActiveTags();
    }

    /**
     * Convert tags to comma-separated string
     */
    public String tagsToString(Set<Tag> tags) {
        return tags.stream()
            .map(Tag::getTagName)
            .sorted()
            .collect(Collectors.joining(", "));
    }
}