package com.cartshare.backend.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SearchUtilsTest {

    @Test
    @DisplayName("Should return empty list for null or blank input")
    void generateSearchKeywords_NullOrBlank() {
        assertThat(SearchUtils.generateSearchKeywords(null)).isEmpty();
        assertThat(SearchUtils.generateSearchKeywords("   ")).isEmpty();
    }

    @Test
    @DisplayName("Should filter out words shorter than 3 characters")
    void generateSearchKeywords_FiltersShortWords() {
        // "O", "do", and "e" are < 3 chars and should be ignored
        List<String> result = SearchUtils.generateSearchKeywords("O Bolo de Mel e Café");

        assertThat(result).containsExactlyInAnyOrder("bolo", "mel", "café", "cafe");
        assertThat(result).doesNotContain("o", "do", "e");
    }

    @Test
    @DisplayName("Should remove special symbols but keep Portuguese characters")
    void generateSearchKeywords_CleansInput() {
        List<String> result = SearchUtils.generateSearchKeywords("Promoção!!!");

        // Should remove '!!!', then generate both versions
        assertThat(result).containsExactlyInAnyOrder("promoção", "promocao");
    }

    @ParameterizedTest
    @MethodSource("provideKeywordScenarios")
    @DisplayName("Should handle complex combinations of accents and normalization")
    void generateSearchKeywords_ComplexScenarios(String input, List<String> expected) {
        List<String> result = SearchUtils.generateSearchKeywords(input);
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> provideKeywordScenarios() {
        return Stream.of(
                // Scenario 1: Basic accent expansion
                Arguments.of("Açaí", List.of("açaí", "acai")),

                // Scenario 2: Multiple words with symbols
                Arguments.of("Pão & Chouriço", List.of("pão", "pao", "chouriço", "chourico")),

                // Scenario 3: Mixed numbers and letters
                Arguments.of("iPhone 15 Pro", List.of("iphone", "pro")), // '15' is < 3 chars

                // Scenario 4: Deduplication (pao and pao)
                Arguments.of("Pão pao", List.of("pão", "pao"))
        );
    }
}