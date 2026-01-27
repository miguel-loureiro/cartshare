package com.cartshare.backend.api.controller;

import com.cartshare.backend.core.service.AutocompleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
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
        // Updated to reflect the lean constructor without CategoryService
        searchController = new SearchController(autocompleteService);
    }

    @Test
    @DisplayName("getSuggestions: Should return exact list provided by service (e.g. pão)")
    void getSuggestions_CallsServiceAndReturnsResult() {
        // Arrange
        String term = "pao";
        List<String> expected = List.of("pão", "pão de queijo");
        when(autocompleteService.suggest(term)).thenReturn(expected);

        // Act
        List<String> result = searchController.getSuggestions(term);

        // Assert
        assertThat(result)
                .hasSize(2)
                .containsExactly("pão", "pão de queijo");
        verify(autocompleteService).suggest(term);
    }

    @Test
    @DisplayName("getSuggestions: Should handle empty results gracefully for unknown terms")
    void getSuggestions_HandlesEmptyResult() {
        // Arrange
        String unknownTerm = "xyz123";
        when(autocompleteService.suggest(unknownTerm)).thenReturn(List.of());

        // Act
        List<String> result = searchController.getSuggestions(unknownTerm);

        // Assert
        assertThat(result).isEmpty();
        verify(autocompleteService).suggest(unknownTerm);
    }
}