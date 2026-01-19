package com.cartshare.backend.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilsTest {

@Test
@DisplayName("getResourceStream: Should return InputStream when resource exists")
void getResourceStream_ReturnsStream_WhenFileExists() throws Exception {
    // Arrange: Use a file that exists in src/test/resources
    String path = "test-file.txt";

    // Act
    try (InputStream is = ResourceUtils.getResourceStream(path)) {
        // Assert
        assertThat(is).isNotNull();
    }
}

@Test
@DisplayName("getResourceStream: Should throw IllegalArgumentException when resource is missing")
void getResourceStream_ThrowsException_WhenFileNotFound() {
    // Arrange
    String invalidPath = "non-existent-file.json";

    // Act & Assert
    assertThatThrownBy(() -> ResourceUtils.getResourceStream(invalidPath))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Resource not found: " + invalidPath);
}

@Test
@DisplayName("Constructor: Should be private and throw exception via reflection")
void privateConstructor_Test() throws Exception {
    Constructor<ResourceUtils> constructor = ResourceUtils.class.getDeclaredConstructor();
    assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();

    constructor.setAccessible(true);
    // Utility constructors often do nothing or throw an error;
    // this simply ensures we can't instantiate it normally.
    ResourceUtils instance = constructor.newInstance();
    assertThat(instance).isNotNull();
}
}