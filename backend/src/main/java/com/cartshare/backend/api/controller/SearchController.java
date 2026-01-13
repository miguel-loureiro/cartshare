package com.cartshare.backend.api.controller;

import com.cartshare.backend.core.service.AutocompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final AutocompleteService autocompleteService;

    @GetMapping("/autocomplete")
    public List<String> getSuggestions(@RequestParam String term) {
        return autocompleteService.suggest(term);
    }

    @GetMapping("/categories")
    public Set<String> getCategories(@RequestParam String keyword) {
        return autocompleteService.getCategoriesForKeyword(keyword);
    }
}
