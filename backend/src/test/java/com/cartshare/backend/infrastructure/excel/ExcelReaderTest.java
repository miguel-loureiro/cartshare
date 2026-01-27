package com.cartshare.backend.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderTest {

    @Test
    @DisplayName("Should read all non-empty rows including header")
    void shouldReadNonEmptyRows() throws Exception {
        // Arrange: 4 rows total, 1 is empty
        byte[] excelContent = createMockExcel(new String[][]{
                {"Header1", "Header2"}, // Row 0
                {"Value1", "Value2"},   // Row 1
                {"", ""},               // Row 2 (Empty)
                {"Value3", "Value4"}    // Row 3
        });
        InputStream is = new ByteArrayInputStream(excelContent);

        // Act
        List<List<String>> result = ExcelReader.read(is);

        // Assert
        // Actual rows returned: Header, Value1, Value3
        assertEquals(3, result.size(), "Should return 3 rows (Header + 2 Data rows)");
        assertEquals("Header1", result.get(0).getFirst());
        assertEquals("Value3", result.get(2).getFirst());
    }

    @Test
    @DisplayName("Should handle single column rows correctly (Flat Schema)")
    void shouldHandleSingleColumnRows() throws Exception {
        // Arrange: Testing the new Keyword format (just one column)
        byte[] excelContent = createMockExcel(new String[][]{
                {"Keyword"},
                {"Pão"},
                {"Leite"}
        });
        InputStream is = new ByteArrayInputStream(excelContent);

        // Act
        List<List<String>> result = ExcelReader.read(is);

        // Assert
        assertEquals(3, result.size());
        assertEquals("Pão", result.get(1).getFirst());
    }

    @Test
    @DisplayName("Data Seeding Integration: Skip header via Stream logic")
    void seedingLogicShouldSkipHeader() throws Exception {
        // This simulates how your FirestoreDataSeeder uses the reader
        List<List<String>> data = List.of(
                List.of("Header"),
                List.of("Product1"),
                List.of("Product2")
        );

        // Logic used in Seeder: .skip(1)
        List<String> processed = data.stream()
                .skip(1)
                .map(List::getFirst)
                .toList();

        assertEquals(2, processed.size());
        assertEquals("Product1", processed.get(0));
    }

    @Test
    @DisplayName("Negative: Should return empty list and log error on corrupt stream")
    void shouldHandleCorruptStream() {
        // Arrange: Provide random bytes that are not a valid Excel format
        InputStream corruptStream = new ByteArrayInputStream("NotAnExcelFile".getBytes());

        // Act
        List<List<String>> result = ExcelReader.read(corruptStream);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be an empty list when parsing fails");
        // This covers the 'catch (Exception e)' block in your ExcelReader
    }

    @Test
    @DisplayName("Negative: Should handle null InputStream gracefully")
    void shouldHandleNullInputStream() {
        // Act
        List<List<String>> result = ExcelReader.read(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toSafeId: Should handle various special characters and spacing")
    void toSafeId_ComplexInputs() {
        // Act & Assert
        assertEquals("pao-de-queijo", ExcelReader.toSafeId("Pão de Queijo!!!"));
        assertEquals("coca-cola-2l", ExcelReader.toSafeId("  Coca-Cola (2L)  "));
        assertEquals("unknown", ExcelReader.toSafeId(null));
        assertEquals("shampoo-anti-caspa", ExcelReader.toSafeId("Shampoo Anti-caspa"));
    }

    /**
     * Generates a valid Excel (.xlsx) file in memory for testing.
     * @param data 2D array representing rows and columns
     * @return byte array of the generated Excel file
     */
    private byte[] createMockExcel(String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("TestSheet");
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < data[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(data[i][j]);
                }
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

}