package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Category;
import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.cartshare.backend.core.service.ProductCategoryMatcher;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FirestoreExcelImporter {

    private static final Logger log = LoggerFactory.getLogger(FirestoreExcelImporter.class);
    private final Firestore firestore;
    @Setter
    private boolean dryRun = false;

    public FirestoreExcelImporter(Firestore firestore) {
        this.firestore = firestore;
    }

    public void importCategories(InputStream is) throws Exception {
        List<List<String>> rows = ExcelReader.read(is);
        int total = rows.size();
        log.info("🚀 Starting Categories import: {} rows", total);

        WriteBatch batch = firestore.batch();
        int count = 0;

        for (int i = 0; i < total; i++) {
            List<String> row = rows.get(i);
            if (row.size() >= 4) {
                String id = ExcelReader.toSafeId(row.get(0));
                Category cat = new Category(id, row.get(1), ExcelReader.toSafeId(row.get(2)), Integer.parseInt(row.get(3)));

                if (!dryRun) {
                    batch.set(firestore.collection("categories").document(id), cat);
                    count++;
                }
            }
            logProgress("Categories", i + 1, total);
        }

        if (!dryRun && count > 0) {
            batch.commit().get();
        }
        log.info("✅ Categories import finished. Records saved: {}", count);
    }

    public void importKeywordsFromList(List<Keyword> keywords) throws Exception {
        int total = keywords.size();
        log.info("🚀 Starting Keywords import: {} items", total);

        WriteBatch batch = firestore.batch();
        int count = 0;

        for (int i = 0; i < total; i++) {
            Keyword kw = keywords.get(i);
            var data = Map.of("keyword", kw.keyword(), "categoryId", kw.categoryId());

            if (!dryRun) {
                batch.set(firestore.collection("keywords").document(kw.keyword()), data);
                count++;
            }

            // Batch limit management
            if (count > 0 && count % 400 == 0 && !dryRun) {
                batch.commit().get();
                batch = firestore.batch();
            }

            logProgress("Keywords", i + 1, total);
        }

        if (!dryRun && count % 400 != 0) {
            batch.commit().get();
        }
        log.info("✅ Keywords import finished. Records saved: {}", count);
    }

    public void importProducts(InputStream is, List<Keyword> keywords) throws Exception {
        List<List<String>> rows = ExcelReader.read(is);
        int total = rows.size();
        log.info("🚀 Starting Products import: {} rows", total);

        WriteBatch batch = firestore.batch();
        int count = 0;

        for (int i = 0; i < total; i++) {
            List<String> r = rows.get(i);
            if (r.size() >= 2) {
                String productName = r.get(0);
                String category = ProductCategoryMatcher.resolveCategory(productName, keywords, r.get(1));
                String docId = ExcelReader.toSafeId(productName);

                var data = Map.of(
                        "productName", productName,
                        "categoryId", category,
                        "searchKeywords", generateSearchKeywords(productName)
                );

                if (!dryRun) {
                    batch.set(firestore.collection("products").document(docId), data);
                    count++;
                }

                if (count > 0 && count % 400 == 0 && !dryRun) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
            logProgress("Products", i + 1, total);
        }

        if (!dryRun && count % 400 != 0) {
            batch.commit().get();
        }
        log.info("✅ Products import finished. Records saved: {}", count);
    }

    // Add this to FirestoreExcelImporter
    public void importCategoriesFromList(List<Category> categories) throws Exception {
        WriteBatch batch = firestore.batch();
        for (Category cat : categories) {
            batch.set(firestore.collection("categories").document(cat.id()), cat);
        }
        batch.commit().get();
    }

    // Add this to FirestoreExcelImporter
    public void importProductsFromList(List<Product> products) throws Exception {
        WriteBatch batch = firestore.batch();
        int count = 0;
        for (Product prod : products) {
            String docId = ExcelReader.toSafeId(prod.productName());
            batch.set(firestore.collection("products").document(docId), prod);

            count++;
            if (count % 400 == 0) {
                batch.commit().get();
                batch = firestore.batch();
            }
        }
        batch.commit().get();
    }

    /**
     * Helper to log progress every 10% or every 100 items (whichever is smaller)
     */
    private void logProgress(String task, int current, int total) {
        int interval = Math.max(1, total / 10); // Log every 10%
        if (current == 1 || current == total || current % interval == 0) {
            double percent = ((double) current / total) * 100;
            log.info("--> {} Progress: {}/{} ({}%)", task, current, total, String.format("%.1f", percent));
        }
    }

    public List<String> generateSearchKeywords(String name) {
        if (name == null || name.isBlank()) return List.of();
        return Arrays.stream(name.toLowerCase().split("\\s+"))
                .map(word -> word.replaceAll("[^a-zA-Z0-9áéíóúâêîôûàèìòùçãõ]", ""))
                .filter(word -> word.length() > 2)
                .distinct()
                .toList();
    }
}
