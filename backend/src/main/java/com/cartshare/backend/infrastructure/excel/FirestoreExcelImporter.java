package com.cartshare.backend.infrastructure.excel;

import com.cartshare.backend.core.model.Keyword;
import com.cartshare.backend.core.model.Product;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FirestoreExcelImporter {

    private static final Logger log = LoggerFactory.getLogger(FirestoreExcelImporter.class);
    private final Firestore firestore;

    @Setter
    private boolean dryRun = false;

    private static final int BATCH_SIZE = 400;

    public FirestoreExcelImporter(Firestore firestore) {
        this.firestore = firestore;
    }

    public void importKeywordsFromList(List<Keyword> keywords) throws Exception {
        int total = keywords.size();
        log.info("🚀 Starting Keywords import: {} items", total);

        WriteBatch batch = dryRun ? null : firestore.batch();
        int count = 0;

        for (int i = 0; i < total; i++) {
            Keyword kw = keywords.get(i);

            // Use the keyword itself (normalized) as the Document ID
            String docId = ExcelReader.toSafeId(kw.keyword());

            if (!dryRun) {
                // By passing the Record 'kw', Firestore uses @DocumentId and @PropertyName
                assert batch != null;
                batch.set(firestore.collection("keywords").document(docId), kw);
                count++;

                if (count % BATCH_SIZE == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
            logProgress(i + 1, total);
        }

        finalizeBatch(batch, count);
        log.info("✅ Keywords import finished. Records saved: {}", count);
    }

    public void importProducts(InputStream is) throws Exception {
        List<List<String>> rows = ExcelReader.read(is);
        WriteBatch batch = firestore.batch();
        int count = 0;

        for (List<String> r : rows) {
            if (!r.isEmpty()) {
                String name = r.getFirst();
                String docId = ExcelReader.toSafeId(name);

                Product product = new Product(
                        docId,
                        name,
                        true, // It's from Excel, so it's official
                        generateSearchKeywords(name)
                );

                batch.set(firestore.collection("products").document(docId), product);
                count++;

                if (count % 400 == 0) { batch.commit().get(); batch = firestore.batch(); }
            }
        }
        batch.commit().get();
    }

    public void importProductsFromList(List<Product> products) throws Exception {
        if (dryRun) return;
        WriteBatch batch = firestore.batch();
        int count = 0;
        for (Product prod : products) {
            String docId = ExcelReader.toSafeId(prod.productName());
            batch.set(firestore.collection("products").document(docId), prod);
            count++;
            if (count % BATCH_SIZE == 0) {
                batch.commit().get();
                batch = firestore.batch();
            }
        }
        if (count > 0 && count % BATCH_SIZE != 0) batch.commit().get();
    }

    /**
     * Requirement: "pão" generates ["pao", "pão"]
     */
    public List<String> generateSearchKeywords(String name) {
        if (name == null || name.isBlank()) return List.of();

        return Arrays.stream(name.toLowerCase().split("\\s+"))
                // Clean: keep only letters/numbers and Portuguese accents (added 'ï' for Naïve test)
                .map(word -> word.replaceAll("[^a-z0-9áéíóúâêîôûàèìòùçãõï]", ""))
                .filter(word -> !word.isBlank())
                .flatMap(word -> {
                    String normalized = stripAccents(word);
                    // Return both original ("pão") and stripped ("pao") versions
                    return Stream.of(word, normalized);
                })
                .filter(word -> word.length() >= 3)
                .distinct()
                .collect(Collectors.toList());
    }

    private String stripAccents(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private void logProgress(int current, int total) {
        int interval = Math.max(1, total / 10);
        if (current == 1 || current == total || current % interval == 0) {
            double percent = ((double) current / total) * 100;
            log.info("--> {} Progress: {}/{} ({}%)", "Keywords", current, total, String.format("%.1f", percent));
        }
    }

    /**
     * Helper to finalize the last batch if it hasn't been committed.
     */
    private void finalizeBatch(WriteBatch batch, int count) throws Exception {
        if (!dryRun && batch != null && count > 0 && count % BATCH_SIZE != 0) {
            batch.commit().get();
        }
    }
}