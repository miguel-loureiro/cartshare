package com.cartshare.backend.api.controller;

import com.cartshare.backend.core.service.ProductExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ProductExportService exportService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(exportService);
    }

    @Test
    @DisplayName("downloadProductBackup: Should return 200 OK with attachment headers")
    void downloadProductBackup_Success() throws Exception {
        // Arrange
        byte[] mockData = "[{\"name\":\"test\"}]".getBytes();
        when(exportService.exportProductsToData()).thenReturn(mockData);

        // Act
        ResponseEntity<byte[]> response = adminController.downloadProductBackup();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment; filename=\"products_backup_");
        assertThat(response.getBody()).isEqualTo(mockData);
    }

    @Test
    @DisplayName("downloadProductBackup: Should return 500 when an exception occurs")
    void downloadProductBackup_Error() throws Exception {
        // Arrange
        when(exportService.exportProductsToData()).thenThrow(new RuntimeException("Export failed"));

        // Act
        ResponseEntity<byte[]> response = adminController.downloadProductBackup();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}