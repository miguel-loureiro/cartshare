package com.cartshare.backend.api.controller;

import com.cartshare.backend.core.service.ProductExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ProductExportService exportService;

    @GetMapping("/export/products")
    public ResponseEntity<byte[]> downloadProductBackup() {
        log.info("📥 Request received to export products backup");
        try {
            byte[] fileContent = exportService.exportProductsToData();
            String fileName = "products_backup_" + System.currentTimeMillis() + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(fileContent);

        } catch (Exception e) {
            log.error("❌ Export failed: ", e);
            // Returning a 500 status with a message is better for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
