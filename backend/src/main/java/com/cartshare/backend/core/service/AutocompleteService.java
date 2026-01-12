package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Category;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutocompleteService {

    private final Map<String, List<String>> index = new ConcurrentHashMap<>();

    // Este método será chamado pelo CatalogSyncService após ler o Excel
    public void indexUpdate(List<Category> categories) {
        index.clear();
        for (Category cat : categories) {
            for (String kw : cat.keywords()) {
                // Mapeia cada palavra-chave para a sua categoria
                index.computeIfAbsent(kw.toLowerCase(), k -> new ArrayList<>())
                        .add(cat.name());
            }
        }
    }

    public List<String> sugest(String termo) {
        String searchString = termo.toLowerCase();
        return index.keySet().stream()
                .filter(k -> k.startsWith(searchString))
                .limit(10)
                .toList();
    }
}
