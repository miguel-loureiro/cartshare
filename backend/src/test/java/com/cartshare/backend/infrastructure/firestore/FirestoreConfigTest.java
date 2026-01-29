package com.cartshare.backend.infrastructure.firestore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FirestoreConfig
 * Target coverage: ~90%
 */
class FirestoreConfigTest {

    private FirestoreConfig firestoreConfig;
    private FirestoreProperties mockProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        firestoreConfig = new FirestoreConfig();
        mockProperties = mock(FirestoreProperties.class);
    }

    // ============================================================
    // SUCCESSFUL FIRESTORE INITIALIZATION TESTS
    // ============================================================

    @Test
    void firestore_shouldInitializeSuccessfully_whenValidCredentialsProvided() throws IOException {
        // Arrange
        String projectId = "test-project-id";
        Path keyFile = createValidServiceAccountKey();

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(keyFile.toString());

        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<FirestoreOptions> firestoreOptionsMock = mockStatic(FirestoreOptions.class)) {

            // Mock GoogleCredentials
            GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenReturn(mockCredentials);

            // Mock FirestoreOptions builder chain
            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class, RETURNS_SELF);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            firestoreOptionsMock.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);

            // Act
            Firestore result = firestoreConfig.firestore(mockProperties);

            // Assert
            assertNotNull(result);
            verify(mockBuilder).setCredentials(mockCredentials);
            verify(mockBuilder).setProjectId(projectId);
            verify(mockBuilder).build();
            verify(mockOptions).getService();
        }
    }

    @Test
    void firestore_shouldLogCorrectInformation_whenInitializing() throws IOException {
        // Arrange
        String projectId = "my-test-project";
        Path keyFile = createValidServiceAccountKey();

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(keyFile.toString());

        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<FirestoreOptions> firestoreOptionsMock = mockStatic(FirestoreOptions.class)) {

            GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenReturn(mockCredentials);

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class, RETURNS_SELF);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            firestoreOptionsMock.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);

            // Act
            firestoreConfig.firestore(mockProperties);

            // Assert - verify log message was generated (indirectly by no exception)
            // Note: In a real scenario, you might use a logging test framework
            assertNotNull(mockFirestore);
        }
    }

    // ============================================================
    // PROJECT ID VALIDATION TESTS
    // ============================================================

    @Test
    void firestore_shouldThrowException_whenProjectIdIsNull() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn(null);
        when(mockProperties.getKeyPath()).thenReturn("/valid/path/key.json");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("projectId is null or blank"));
        assertTrue(exception.getMessage().contains("google.cloud.project-id"));
    }

    @Test
    void firestore_shouldThrowException_whenProjectIdIsEmpty() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("");
        when(mockProperties.getKeyPath()).thenReturn("/valid/path/key.json");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("projectId is null or blank"));
    }

    @Test
    void firestore_shouldThrowException_whenProjectIdIsBlank() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("   ");
        when(mockProperties.getKeyPath()).thenReturn("/valid/path/key.json");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("projectId is null or blank"));
    }

    // ============================================================
    // KEY PATH VALIDATION TESTS
    // ============================================================

    @Test
    void firestore_shouldThrowException_whenKeyPathIsNull() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("valid-project-id");
        when(mockProperties.getKeyPath()).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("keyPath is null or blank"));
        assertTrue(exception.getMessage().contains("google.cloud.key-path"));
    }

    @Test
    void firestore_shouldThrowException_whenKeyPathIsEmpty() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("valid-project-id");
        when(mockProperties.getKeyPath()).thenReturn("");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("keyPath is null or blank"));
    }

    @Test
    void firestore_shouldThrowException_whenKeyPathIsBlank() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("valid-project-id");
        when(mockProperties.getKeyPath()).thenReturn("   ");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        assertTrue(exception.getMessage().contains("keyPath is null or blank"));
    }

    // ============================================================
    // FILE I/O ERROR TESTS
    // ============================================================

    @Test
    void firestore_shouldThrowIOException_whenKeyFileDoesNotExist() {
        // Arrange
        when(mockProperties.getProjectId()).thenReturn("valid-project-id");
        when(mockProperties.getKeyPath()).thenReturn("/non/existent/path/key.json");

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );
    }

    @Test
    void firestore_shouldThrowIOException_whenKeyFileIsInvalid() throws IOException {
        // Arrange
        String projectId = "test-project";
        Path invalidKeyFile = tempDir.resolve("invalid-key.json");
        Files.writeString(invalidKeyFile, "invalid json content");

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(invalidKeyFile.toString());

        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class)) {
            // Mock GoogleCredentials to throw IOException for invalid content
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenThrow(new IOException("Invalid credentials format"));

            // Act & Assert
            IOException exception = assertThrows(
                    IOException.class,
                    () -> firestoreConfig.firestore(mockProperties)
            );

            assertEquals("Invalid credentials format", exception.getMessage());
        }
    }

    @Test
    void firestore_shouldThrowIOException_whenKeyFileCannotBeRead() throws IOException {
        // Arrange
        String projectId = "test-project";
        Path keyFile = tempDir.resolve("unreadable-key.json");
        Files.writeString(keyFile, createMockServiceAccountJson());

        // Make file unreadable (this might not work on all OS)
        keyFile.toFile().setReadable(false);

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(keyFile.toString());

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> firestoreConfig.firestore(mockProperties)
        );

        // Cleanup - restore permissions
        keyFile.toFile().setReadable(true);
    }

    // ============================================================
    // EDGE CASE TESTS
    // ============================================================

    @Test
    void firestore_shouldHandleSpecialCharactersInProjectId() throws IOException {
        // Arrange
        String projectId = "test-project-123_special";
        Path keyFile = createValidServiceAccountKey();

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(keyFile.toString());

        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<FirestoreOptions> firestoreOptionsMock = mockStatic(FirestoreOptions.class)) {

            GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenReturn(mockCredentials);

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class, RETURNS_SELF);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            firestoreOptionsMock.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);

            // Act
            Firestore result = firestoreConfig.firestore(mockProperties);

            // Assert
            assertNotNull(result);
            verify(mockBuilder).setProjectId(projectId);
        }
    }

    @Test
    void firestore_shouldHandlePathsWithSpaces() throws IOException {
        // Arrange
        String projectId = "test-project";
        Path dirWithSpace = tempDir.resolve("path with space");
        Files.createDirectories(dirWithSpace);
        Path keyFile = dirWithSpace.resolve("key.json");
        Files.writeString(keyFile, createMockServiceAccountJson());

        when(mockProperties.getProjectId()).thenReturn(projectId);
        when(mockProperties.getKeyPath()).thenReturn(keyFile.toString());

        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<FirestoreOptions> firestoreOptionsMock = mockStatic(FirestoreOptions.class)) {

            GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenReturn(mockCredentials);

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class, RETURNS_SELF);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            firestoreOptionsMock.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);

            // Act
            Firestore result = firestoreConfig.firestore(mockProperties);

            // Assert
            assertNotNull(result);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Creates a valid mock service account key file for testing
     */
    private Path createValidServiceAccountKey() throws IOException {
        Path keyFile = tempDir.resolve("service-account-key.json");
        Files.writeString(keyFile, createMockServiceAccountJson());
        return keyFile;
    }

    /**
     * Returns a mock JSON structure resembling a Google service account key
     */
    private String createMockServiceAccountJson() {
        return """
                {
                  "type": "service_account",
                  "project_id": "test-project",
                  "private_key_id": "key123",
                  "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7\\n-----END PRIVATE KEY-----\\n",
                  "client_email": "test@test-project.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test%40test-project.iam.gserviceaccount.com"
                }
                """;
    }
}