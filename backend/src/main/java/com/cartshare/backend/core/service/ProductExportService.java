package com.cartshare.backend.core.service;

import com.cartshare.backend.core.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductExportService {

    private final ProductContributionService productContributionService;
    private final ObjectMapper objectMapper;

    /**
     * Fetches all products and converts them to a pretty-printed JSON byte array.
     */
    public byte[] exportProductsToData() throws Exception {
        log.info("🚀 Starting full product export to JSON...");

        List<Product> products = productContributionService.getAllProducts();

        // Convert the list of Product records to a byte array (JSON)
        byte[] jsonData = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(products);

        log.info("✅ Export complete. Generated {} bytes of product data.", jsonData.length);
        return jsonData;
    }
}
