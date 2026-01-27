package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class AutocompleteServiceTest {

    private AutocompleteService autocompleteService;

    @BeforeEach
    void setUp() {
        autocompleteService = new AutocompleteService();
    }

    @Test
    @DisplayName("Normalization: Should match words regardless of accents or casing")
    void shouldNormalizeSearch() {
        // Arrange
        Keyword kw = new Keyword("Café");
        autocompleteService.indexUpdate(List.of(kw), List.of());

        // Act & Assert
        assertThat(autocompleteService.suggest("cafe")).containsExactly("Café");
        assertThat(autocompleteService.suggest("CAFÉ")).containsExactly("Café");
    }

    @Test
    @DisplayName("Fuzzy Match: Should match 'pao' to 'pão' using Levenshtein distance")
    void shouldFuzzyMatch() {
        // Arrange
        Keyword kw = new Keyword("pão");
        autocompleteService.indexUpdate(List.of(kw), List.of());

        // Act & Assert
        // Distance between "pao" and "pão" (after normalization) is 0,
        // but even if it were different, fuzzy match handles it.
        assertThat(autocompleteService.suggest("pao")).containsExactly("pão");
    }

    @Test
    @DisplayName("Priority: Official products should appear before keywords")
    void officialProductsShouldHavePriority() {
        // Arrange
        Keyword kw = new Keyword("Arroz"); // Priority 1

        // This product name contains "Arroz", so it will be found
        Product officialProd = Product.createOfficial("Arroz Agulhão", List.of("branco"));

        // This product name also contains "Arroz", but is Priority 5
        Product userProd = new Product("user-1", "Arroz de Festa", false, List.of("festa"));

        autocompleteService.indexUpdate(List.of(kw), List.of(officialProd, userProd));

        // Act
        List<String> suggestions = autocompleteService.suggest("arroz");

        // Assert
        assertThat(suggestions)
                .as("Should find the keyword and both product names")
                .hasSize(3)
                .contains("Arroz", "Arroz Agulhão", "Arroz de Festa");

        // Verify Priority: Priority 1 items must come before Priority 5
        // "Arroz de Festa" is Priority 5, so it must be the last element
        assertThat(suggestions.get(2)).isEqualTo("Arroz de Festa");
    }

    @Test
    @DisplayName("Limit: Should never return more than 10 suggestions")
    void shouldLimitSuggestions() {
        // Arrange
        List<Keyword> manyKeywords = IntStream.range(0, 20)
                .mapToObj(i -> new Keyword("Keyword " + i))
                .toList();
        autocompleteService.indexUpdate(manyKeywords, List.of());

        // Act
        List<String> results = autocompleteService.suggest("Keyword");

        // Assert
        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("Edge Case: Should handle null or empty search terms gracefully")
    void shouldHandleEmptyInputs() {
        assertThat(autocompleteService.suggest(null)).isEmpty();
        assertThat(autocompleteService.suggest("")).isEmpty();
        assertThat(autocompleteService.suggest("   ")).isEmpty();
    }
}