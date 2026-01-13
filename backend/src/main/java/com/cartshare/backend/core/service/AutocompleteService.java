package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AutocompleteService {

    private record KeywordMetadata(int priority, Set<String> categoryIds) {}
    private final Map<String, KeywordMetadata> masterIndex = new ConcurrentHashMap<>();
    private static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public void indexUpdate(List<Category> categories, List<Keyword> keywords, List<Product> products) {
        Map<String, KeywordMetadata> newIndex = new HashMap<>();
        Map<String, Integer> priorityLookup = categories.stream()
                .collect(Collectors.toMap(Category::id, Category::priority, (a, b) -> a));

        for (Keyword kw : keywords) {
            mergeIntoLocalMap(newIndex, kw.keyword(), priorityLookup.getOrDefault(kw.categoryId(), 99), kw.categoryId());
        }

        for (Product prod : products) {
            int priority = priorityLookup.getOrDefault(prod.categoryId(), 99);
            for (String term : prod.searchKeywords()) {
                mergeIntoLocalMap(newIndex, term, priority, prod.categoryId());
            }
        }

        masterIndex.clear();
        masterIndex.putAll(newIndex);
    }

    public List<String> suggest(String term) {
        if (term == null || term.isBlank()) return List.of();
        String query = normalize(term);

        // 1. Prefix matches (normalized)
        List<String> prefixMatches = masterIndex.entrySet().stream()
                .filter(e -> normalize(e.getKey()).startsWith(query))
                .sorted(Comparator.<Map.Entry<String, KeywordMetadata>>comparingInt(e -> e.getValue().priority())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .limit(10)
                .toList();

        if (prefixMatches.size() >= 5 || query.length() < 3) return prefixMatches;

        // 2. Fuzzy matches (normalized Levenshtein)
        List<String> fuzzyMatches = masterIndex.entrySet().stream()
                .filter(e -> !normalize(e.getKey()).startsWith(query))
                .filter(e -> isFuzzyMatch(query, normalize(e.getKey())))
                .sorted(Comparator.<Map.Entry<String, KeywordMetadata>>comparingInt(e -> e.getValue().priority()))
                .map(Map.Entry::getKey)
                .limit(10 - prefixMatches.size())
                .toList();

        List<String> combined = new ArrayList<>(prefixMatches);
        combined.addAll(fuzzyMatches);
        return combined;
    }

    private String normalize(String input) {
        if (input == null) return "";
        String nfdNormalizedString = Normalizer.normalize(input.toLowerCase().trim(), Normalizer.Form.NFD);
        return ACCENT_PATTERN.matcher(nfdNormalizedString).replaceAll("");
    }

    private boolean isFuzzyMatch(String query, String target) {
        int allowedDistance = query.length() > 4 ? 2 : 1;
        return calculateLevenshteinDistance(query, target) <= allowedDistance;
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            int[] curr = new int[s2.length() + 1];
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int d = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + d);
            }
            prev = curr;
        }
        return prev[s2.length()];
    }

    private void mergeIntoLocalMap(Map<String, KeywordMetadata> map, String term, int priority, String categoryId) {
        if (term == null || term.isBlank()) return;
        String key = term.toLowerCase().trim();
        map.compute(key, (k, existing) -> {
            if (existing == null) {
                Set<String> ids = new HashSet<>(List.of(categoryId));
                return new KeywordMetadata(priority, ids);
            } else {
                existing.categoryIds().add(categoryId);
                return new KeywordMetadata(Math.min(existing.priority(), priority), existing.categoryIds());
            }
        });
    }

    public Set<String> getCategoriesForKeyword(String keyword) {
        if (keyword == null) return Set.of();
        KeywordMetadata metadata = masterIndex.get(keyword.toLowerCase().trim());
        return metadata != null ? metadata.categoryIds() : Set.of();
    }
}