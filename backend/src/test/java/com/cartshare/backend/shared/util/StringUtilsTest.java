package com.cartshare.backend.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    @DisplayName("Should return null when input is null")
    void toSafeId_ReturnsNull_WhenInputIsNull() {
        assertThat(StringUtils.toSafeId(null)).isNull();
    }

    @Test
    @DisplayName("Should return empty string when input is empty")
    void toSafeId_ReturnsEmpty_WhenInputIsEmpty() {
        assertThat(StringUtils.toSafeId("")).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Should correctly transform various strings into safe IDs")
    @CsvSource({
            "Pão Caseiro, paocaseiro",       // Mixed case, space, and accent
            "Café EXPRESSO!, cafeexpresso",   // Upper case, accent, and punctuation
            "123-ABC, 123abc",               // Numbers and symbols
            "  Trim Test  , trimtest",       // Leading/trailing spaces
            "música & dança, musicadanca",   // Ampersand and special chars
            "Açaí, acai"                     // Cedilla and accent
    })
    void toSafeId_TransformsCorrectly(String input, String expected) {
        String result = StringUtils.toSafeId(input);
        assertThat(result).isEqualTo(expected);
    }
}