package com.cartshare.backend.shared.util;

public class StringUtils {
    public static String toSafeId(String input) {
        if (input == null) return null;
        return java.text.Normalizer.normalize(input.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")       // Remove accents (ã -> a)
                .replaceAll("[^a-z0-9]", "")   // Remove spaces and symbols (pão caseiro -> paocaseiro)
                .trim();
    }
}
