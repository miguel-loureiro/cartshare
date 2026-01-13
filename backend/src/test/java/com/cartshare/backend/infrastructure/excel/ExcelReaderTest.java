package com.cartshare.backend.infrastructure.excel;

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
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - ExcelReader")
class ExcelReaderTest {

    @Nested
    @DisplayName("Tests for toSafeId (Sanitization)")
    class ToSafeIdTests {

        @ParameterizedTest
        @CsvSource({
                "Alimentação, ALIMENTAÇÃO",      // Preserves Ã and Ç
                "Padaria e Panificação, PADARIA_E_PANIFICAÇÃO",
                "Frutos & Grãos, FRUTOS__GRÃOS", // Keeps Ã, removes &
                "Maçã, MAÇÃ",
                "  Espaço  , ESPAÇO",
                "Promoção!, PROMOÇÃO"            // Keeps ÇÃ, removes !
        })
        @DisplayName("Should convert strings to CAPS_SNAKE_CASE correctly")
        void shouldConvertAndSanitizeStrings(String input, String expected) {
            assertEquals(expected, ExcelReader.toSafeId(input));
        }

        @DisplayName("Should preserve Portuguese characters while sanitizing")
        void shouldKeepPortugueseAccents(String input, String expected) {
            assertEquals(expected, ExcelReader.toSafeId(input));
        }

        @Test
        @DisplayName("Should handle null input gracefully")
        void shouldHandleNullInput() {
            assertEquals("", ExcelReader.toSafeId(null));
        }

        @Test
        @DisplayName("Should still remove generic special characters")
        void shouldStillRemoveSymbols() {
            String input = "Café @ Home #2026";
            // E is preserved, spaces become underscores, @ and # are removed
            String expected = "CAFÉ__HOME_2026";
            assertEquals(expected, ExcelReader.toSafeId(input));
        }
    }

    @Nested
    @DisplayName("Tests for read(InputStream)")
    class ReadTests {

        @Test
        @DisplayName("Should skip header and read valid data rows")
        void shouldReadExcelDataCorrectly() throws Exception {
            // Arrange: Create a mock Excel file in memory
            byte[] excelContent = createMockExcel(new String[][]{
                    {"ID", "Name"},      // Header (Row 0)
                    {"ALIMENTOS", "Comida"}, // Data (Row 1)
                    {"LIMPEZA", "Limpar"}    // Data (Row 2)
            });
            InputStream is = new ByteArrayInputStream(excelContent);

            // Act
            List<List<String>> result = ExcelReader.read(is);

            // Assert
            assertAll("Excel Reading Results",
                    () -> assertEquals(2, result.size(), "Should have read 2 rows (excluding header)"),
                    () -> assertEquals("ALIMENTOS", result.get(0).get(0)),
                    () -> assertEquals("Comida", result.get(0).get(1)),
                    () -> assertEquals("LIMPEZA", result.get(1).get(0))
            );
        }

        @Test
        @DisplayName("Should ignore empty rows")
        void shouldIgnoreEmptyRows() throws Exception {
            // Arrange: Row 2 is empty
            byte[] excelContent = createMockExcel(new String[][]{
                    {"Header1", "Header2"},
                    {"Value1", "Value2"},
                    {"", ""},
                    {"Value3", "Value4"}
            });
            InputStream is = new ByteArrayInputStream(excelContent);

            // Act
            List<List<String>> result = ExcelReader.read(is);

            // Assert
            assertEquals(2, result.size(), "Should only read non-empty rows");
        }
    }

    /**
     * Helper method to generate an Excel file in memory for testing.
     */
    private byte[] createMockExcel(String[][] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Test");
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < data[i].length; j++) {
                    row.createCell(j).setCellValue(data[i][j]);
                }
            }
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}