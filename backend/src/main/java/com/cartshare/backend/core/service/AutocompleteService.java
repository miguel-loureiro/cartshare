package com.cartshare.backend.core.service;

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
    // We only need to map the Keyword to its priority now
    private final Map<String, Integer> masterIndex = new ConcurrentHashMap<>();
    private static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public void indexUpdate(List<Keyword> keywords, List<Product> products) {
        Map<String, Integer> newIndex = new HashMap<>();

        // 1. Index Keywords
        for (Keyword kw : keywords) {
            newIndex.merge(kw.keyword().trim(), 1, Math::min);
        }

        // 2. Index Products
        for (Product prod : products) {
            int priority = prod.isOfficial() ? 1 : 5;

            // CRITICAL: Index the product name itself!
            newIndex.merge(prod.productName().trim(), priority, Math::min);

            // Also index the search keywords for tags/fuzzy matching
            for (String term : prod.searchKeywords()) {
                newIndex.merge(term.trim(), priority, Math::min);
            }
        }

        masterIndex.clear();
        masterIndex.putAll(newIndex);
    }

    public List<String> suggest(String term) {
        if (term == null || term.isBlank()) return List.of();
        String query = normalize(term);

        return masterIndex.entrySet().stream()
                .filter(e -> normalize(e.getKey()).contains(query) || isFuzzyMatch(query, normalize(e.getKey())))
                .sorted(Map.Entry.comparingByValue()) // Sort by priority (1 comes first)
                .map(Map.Entry::getKey)
                .limit(10)
                .toList();
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
}