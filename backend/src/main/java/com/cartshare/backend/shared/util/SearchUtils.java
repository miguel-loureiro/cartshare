package com.cartshare.backend.shared.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchUtils {

    public static List<String> generateSearchKeywords(String name) {
        if (name == null || name.isBlank()) return List.of();

        return Arrays.stream(name.toLowerCase().split("\\s+"))
                // Clean: keep only letters/numbers and Portuguese accents
                .map(word -> word.replaceAll("[^a-z0-9áéíóúâêîôûàèìòùçãõï]", ""))
                .filter(word -> word.length() >= 3) // Only words with 3+ chars
                .flatMap(word -> {
                    String normalized = stripAccents(word);
                    // Return both original ("pão") and stripped ("pao")
                    return Stream.of(word, normalized);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private static String stripAccents(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
