package com.cartshare.backend.infrastructure.excel;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ExcelReader {
    private ExcelReader() {}

    /**
     * Sanitizes a string to be used as a Firestore ID (CAPS_SNAKE_CASE).
     * Preserves Portuguese characters like Ã and Ç.
     */
    public static String toSafeId(String value) {
        if (value == null) return "";
        return value.trim()
                .toUpperCase()
                .replace(" ", "_")
                // Removes special characters but KEEPS Portuguese accents/letters
                .replaceAll("[^A-Z0-9_ÁÉÍÓÚÂÊÎÔÛÀÈÌÒÙÇÃÕ]", "");
    }

    public static List<List<String>> read(InputStream inputStream) throws Exception {
        try (var wb = WorkbookFactory.create(inputStream)) {
            var sheet = wb.getSheetAt(0);
            List<List<String>> data = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            // Start at 1 to skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                List<String> values = new ArrayList<>();
                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    values.add(formatter.formatCellValue(cell).trim());
                }

                // Only add row if it contains at least one non-empty value
                if (values.stream().anyMatch(v -> !v.isEmpty())) {
                    data.add(values);
                }
            }
            return data;
        }
    }
}

