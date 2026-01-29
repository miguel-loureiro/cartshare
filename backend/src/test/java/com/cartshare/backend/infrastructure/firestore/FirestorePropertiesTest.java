package com.cartshare.backend.infrastructure.firestore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FirestoreProperties
 * Tests the configuration properties binding
 */
@SpringBootTest(classes = FirestoreProperties.class)
@EnableConfigurationProperties(FirestoreProperties.class)
class FirestorePropertiesTest {

    private FirestoreConfig firestoreConfig;
    private FirestoreProperties properties;

    @BeforeEach
    void setUp() {
        firestoreConfig = new FirestoreConfig();
        properties = new FirestoreProperties();
    }

    @Test
    void firestoreProperties_shouldHaveGettersAndSetters() {
        // Arrange
        FirestoreProperties properties = new FirestoreProperties();
        String expectedProjectId = "test-project-123";
        String expectedKeyPath = "/path/to/key.json";

        // Act
        properties.setProjectId(expectedProjectId);
        properties.setKeyPath(expectedKeyPath);

        // Assert
        assertEquals(expectedProjectId, properties.getProjectId());
        assertEquals(expectedKeyPath, properties.getKeyPath());
    }

    @Test
    void firestoreProperties_shouldAllowNullValues() {
        // Arrange
        FirestoreProperties properties = new FirestoreProperties();

        // Act
        properties.setProjectId(null);
        properties.setKeyPath(null);

        // Assert
        assertNull(properties.getProjectId());
        assertNull(properties.getKeyPath());
    }

    @Test
    void firestoreProperties_shouldAllowEmptyStrings() {
        // Arrange
        FirestoreProperties properties = new FirestoreProperties();

        // Act
        properties.setProjectId("");
        properties.setKeyPath("");

        // Assert
        assertEquals("", properties.getProjectId());
        assertEquals("", properties.getKeyPath());
    }

    @Test
    void firestoreProperties_shouldHandleSpecialCharacters() {
        // Arrange
        FirestoreProperties properties = new FirestoreProperties();
        String projectIdWithSpecial = "test-project_123-special";
        String pathWithSpecial = "/path/with space/key-file_123.json";

        // Act
        properties.setProjectId(projectIdWithSpecial);
        properties.setKeyPath(pathWithSpecial);

        // Assert
        assertEquals(projectIdWithSpecial, properties.getProjectId());
        assertEquals(pathWithSpecial, properties.getKeyPath());
    }

    @Test
    void toString_shouldReturnCorrectFormat() {
        FirestoreProperties properties = new FirestoreProperties();
        properties.setProjectId("dev-project");
        properties.setKeyPath("key.json");

        String result = properties.toString();

        // Verifica se o toString contém os campos necessários
        assertTrue(result.contains("projectId='dev-project'"));
        assertTrue(result.contains("keyPath='key.json'"));
    }

    @Test
    void shouldThrowExceptionWhenProjectIdIsMissing() {
        // Cenário: ProjectId está nulo
        properties.setProjectId(null);
        properties.setKeyPath("some-path.json");

        // Verificação: Deve lançar IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            firestoreConfig.firestore(properties);
        });

        assertTrue(exception.getMessage().contains("projectId is null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenKeyPathIsMissing() {
        // Cenário: KeyPath está vazio
        properties.setProjectId("my-project");
        properties.setKeyPath("");

        // Verificação
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            firestoreConfig.firestore(properties);
        });

        assertTrue(exception.getMessage().contains("keyPath is null or blank"));
    }

    @Test
    void shouldThrowIOExceptionWhenFileDoesNotExist() {
        // Cenário: Caminho do ficheiro é válido na string, mas o ficheiro não existe
        properties.setProjectId("my-project");
        properties.setKeyPath("invalid/path/to/key.json");

        // Verificação: O Java tentará abrir o FileInputStream e lançará IOException
        assertThrows(IOException.class, () -> {
            firestoreConfig.firestore(properties);
        });
    }
}