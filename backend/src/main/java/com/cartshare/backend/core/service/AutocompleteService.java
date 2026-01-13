package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutocompleteService {

    // Maps a keyword string to a set of Category IDs (CAPS)
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();

    /**
     * Updates the index using the dedicated Keyword records.
     * This is called after the Excel import or when the Admin Dashboard approves new items.
     */
    public void indexUpdate(List<Keyword> keywords, List<Product> products) {
        index.clear();

        // 1. Index official Keywords from keywords.xlsx
        for (Keyword kw : keywords) {
            index.computeIfAbsent(kw.keyword().toLowerCase(), k -> new HashSet<>())
                    .add(kw.categoryId());
        }

        // 2. Index Product-based keywords (searchKeywords array)
        // This ensures "Dragon Fruit" is discoverable even without a manual keyword
        for (Product prod : products) {
            for (String searchTerm : prod.searchKeywords()) {
                index.computeIfAbsent(searchTerm.toLowerCase(), k -> new HashSet<>())
                        .add(prod.categoryId());
            }
        }
    }

    /**
     * Suggests keywords based on user input.
     * Returns a list of matching keyword strings.
     */
    public List<String> suggest(String term) {
        if (term == null || term.isBlank()) return List.of();

        String searchString = term.toLowerCase().trim();
        return index.keySet().stream()
                .filter(k -> k.startsWith(searchString))
                .sorted() // Alphabetical order for better UX
                .limit(10)
                .toList();
    }

    /**
     * Optional: Returns the Category IDs associated with a specific selected keyword.
     */
    public Set<String> getCategoriesForKeyword(String keyword) {
        return index.getOrDefault(keyword.toLowerCase(), Set.of());
    }
}