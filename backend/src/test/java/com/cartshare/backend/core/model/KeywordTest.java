package com.cartshare.backend.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class KeywordTest {

    @Test
    @DisplayName("Should successfully create a Keyword record when valid arguments are provided")
    void shouldCreateKeyword() {
        // Arrange
        String expectedKeyword = "java-programming";
        String expectedCategory = "tech-001";

        // Act
        Keyword keyword = new Keyword(expectedKeyword, expectedCategory);

        // Assert
        assertThat(keyword.keyword()).isEqualTo(expectedKeyword);
        assertThat(keyword.categoryId()).isEqualTo(expectedCategory);
    }

    @Test
    @DisplayName("Should throw NullPointerException when categoryId is null")
    void shouldThrowExceptionWhenCategoryIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new Keyword("test-keyword", null);
        }, "categoryId is marked @NonNull but did not throw exception on null");
    }

    @Test
    @DisplayName("Should verify equality and hashcode for identical data")
    void testEquality() {
        Keyword k1 = new Keyword("cloud", "infrastructure");
        Keyword k2 = new Keyword("cloud", "infrastructure");

        assertThat(k1).isEqualTo(k2);
        assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
    }

    @Test
    @DisplayName("Should verify inequality for different data")
    void testInequality() {
        Keyword k1 = new Keyword("cloud", "infrastructure");
        Keyword k2 = new Keyword("edge", "infrastructure");

        assertThat(k1).isNotEqualTo(k2);
    }
}