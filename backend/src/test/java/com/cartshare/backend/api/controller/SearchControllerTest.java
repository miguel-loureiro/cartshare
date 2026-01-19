package com.cartshare.backend.api.controller;

import com.cartshare.backend.core.service.AutocompleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private AutocompleteService autocompleteService;

    private SearchController searchController;

    @BeforeEach
    void setUp() {
        // Manually injecting the mock into the controller
        searchController = new SearchController(autocompleteService);
    }

    @Test
    @DisplayName("getSuggestions: Should return exact list provided by service")
    void getSuggestions_CallsServiceAndReturnsResult() {
        // Arrange
        String term = "pao";
        List<String> expected = List.of("pão", "pão de queijo");
        when(autocompleteService.suggest(term)).thenReturn(expected);

        // Act
        List<String> result = searchController.getSuggestions(term);

        // Assert
        assertThat(result).hasSize(2).containsSequence("pão", "pão de queijo");
        verify(autocompleteService).suggest(term);
    }

    @Test
    @DisplayName("getCategories: Should return exact set provided by service")
    void getCategories_CallsServiceAndReturnsResult() {
        // Arrange
        String keyword = "pão";
        Set<String> expected = Set.of("bakery", "dairy");
        when(autocompleteService.getCategoriesForKeyword(keyword)).thenReturn(expected);

        // Act
        Set<String> result = searchController.getCategories(keyword);

        // Assert
        assertThat(result).containsExactlyInAnyOrder("bakery", "dairy");
        verify(autocompleteService).getCategoriesForKeyword(keyword);
    }

    @Test
    @DisplayName("getSuggestions: Should handle empty results gracefully")
    void getSuggestions_HandlesEmptyResult() {
        // Arrange
        when(autocompleteService.suggest("unknown")).thenReturn(List.of());

        // Act
        List<String> result = searchController.getSuggestions("unknown");

        // Assert
        assertThat(result).isEmpty();
    }
}