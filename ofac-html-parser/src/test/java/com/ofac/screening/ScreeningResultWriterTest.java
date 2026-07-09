package com.ofac.screening;

import com.ofac.parser.model.HitDetail;
import com.ofac.parser.model.OFACQueryResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ScreeningResultWriterTest {

    private final ScreeningResultWriter writer = new ScreeningResultWriter();

    // ========== sanitizeSheetName() tests ==========

    @Test
    void sanitizeSheetNameReplacesIllegalChars() {
        String result = ScreeningResultWriter.sanitizeSheetName("test[]:file?*name");
        assertEquals("test___file__name", result);
    }

    @Test
    void sanitizeSheetNameTruncatesLongNames() {
        String longName = "a very long name that exceeds the thirty one character limit";
        String result = ScreeningResultWriter.sanitizeSheetName(longName);
        assertTrue(result.length() <= 31);
        assertEquals(31, result.length());
        assertEquals("a very long name that exceeds t", result);
    }

    @Test
    void sanitizeSheetNameReturnsUnknownForNull() {
        assertEquals("Unknown", ScreeningResultWriter.sanitizeSheetName(null));
    }

    @Test
    void sanitizeSheetNameReturnsSheetForEmptyName() {
        assertEquals("Sheet", ScreeningResultWriter.sanitizeSheetName("   "));
    }

    @Test
    void sanitizeSheetNameLeavesValidNames() {
        String name = "陈平";
        assertEquals("陈平", ScreeningResultWriter.sanitizeSheetName(name));
    }

    @Test
    void sanitizeSheetNameHandlesBackslash() {
        String result = ScreeningResultWriter.sanitizeSheetName("test\\name");
        assertEquals("test_name", result);
    }

    @Test
    void sanitizeSheetNameTruncatesChineseName() {
        // 32 Chinese characters (each is 1 char, so total > 31)
        String longName = String.join("", Collections.nCopies(32, "名"));
        String result = ScreeningResultWriter.sanitizeSheetName(longName);
        assertTrue(result.length() <= 31);
    }

    // ========== Integration: write to temp file ==========

    @Test
    void writeNoHitResultCreatesSheetWithNoHitsMessage() throws Exception {
        ScreeningResults results = new ScreeningResults();
        OFACQueryResult queryResult = new OFACQueryResult(false, "FOL-TEST-001");
        queryResult.setQueryName("Test Name");
        queryResult.setListDate("2026-01-01");
        results.add(new NameResult("Test Name", queryResult));

        File tempFile = File.createTempFile("ofac-test-", ".xlsx");
        try {
            writer.write(results, tempFile.getAbsolutePath());

            // Verify the output file
            try (Workbook wb = new XSSFWorkbook(new FileInputStream(tempFile))) {
                Sheet sheet = wb.getSheet("Test Name");
                assertNotNull(sheet, "Sheet should exist");
                // Row 0: query name metadata; Row 1: hit detected; ...
                Row row0 = sheet.getRow(0);
                assertNotNull(row0);
                assertTrue(row0.getCell(0).getStringCellValue().contains("Query Name"));
                Row noHitRow = sheet.getRow(sheet.getLastRowNum());
                assertTrue(noHitRow.getCell(0).getStringCellValue().contains("No hits found"));
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void writeErrorResultCreatesSheetWithErrorMessage() throws Exception {
        ScreeningResults results = new ScreeningResults();
        results.add(new NameResult("Error Name", "Connection timeout"));

        File tempFile = File.createTempFile("ofac-test-", ".xlsx");
        try {
            writer.write(results, tempFile.getAbsolutePath());

            try (Workbook wb = new XSSFWorkbook(new FileInputStream(tempFile))) {
                Sheet sheet = wb.getSheet("Error Name");
                assertNotNull(sheet);
                Row errRow = sheet.getRow(sheet.getLastRowNum());
                assertTrue(errRow.getCell(0).getStringCellValue().contains("Error occurred"));
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void writeHitResultCreatesSheetWithDetailTable() throws Exception {
        ScreeningResults results = new ScreeningResults();
        OFACQueryResult queryResult = new OFACQueryResult(true, "FOL-TEST-002");
        queryResult.setQueryName("Chen Ping");
        queryResult.setTotalHits(1);

        // Create a hit detail
        HitDetail detail = new HitDetail();
        detail.setHitNumber(1);
        detail.setId("FA1234567");
        detail.setOrigin("FACTIVA");
        detail.setDesignation("OTHER");
        detail.setNames(Collections.singletonList("Chen Ping"));
        detail.setType("Individual");
        detail.setCategories("PEP");
        queryResult.setHitDetails(Collections.singletonList(detail));

        results.add(new NameResult("Chen Ping", queryResult));

        File tempFile = File.createTempFile("ofac-test-", ".xlsx");
        try {
            writer.write(results, tempFile.getAbsolutePath());

            try (Workbook wb = new XSSFWorkbook(new FileInputStream(tempFile))) {
                Sheet sheet = wb.getSheet("Chen Ping");
                assertNotNull(sheet);

                // Check metadata
                Row row0 = sheet.getRow(0);
                assertTrue(row0.getCell(0).getStringCellValue().contains("Query Name"));

                // Check detail header row
                // The detail table starts after metadata + separator
                Row headerRow = findRowContaining(sheet, "Field \\ Hit #");
                assertNotNull(headerRow, "Should contain detail header");
                assertEquals("Hit #1", headerRow.getCell(1).getStringCellValue());

                // Check data row for ID field (exact match, not "Message ID" or "List Date")
                Row idRow = findRowWithExactLabel(sheet, "ID");
                assertNotNull(idRow, "Should contain ID field row");
                assertEquals("FA1234567", idRow.getCell(1).getStringCellValue());
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void writeMultipleResultsCreatesMultipleSheets() throws Exception {
        ScreeningResults results = new ScreeningResults();
        results.add(new NameResult("Name One", new OFACQueryResult(false, "MSG1")));
        results.add(new NameResult("Name Two", new OFACQueryResult(false, "MSG2")));

        File tempFile = File.createTempFile("ofac-test-", ".xlsx");
        try {
            writer.write(results, tempFile.getAbsolutePath());

            try (Workbook wb = new XSSFWorkbook(new FileInputStream(tempFile))) {
                assertNotNull(wb.getSheet("Name One"));
                assertNotNull(wb.getSheet("Name Two"));
                assertEquals(2, wb.getNumberOfSheets());
            }
        } finally {
            tempFile.delete();
        }
    }

    private Row findRowContaining(Sheet sheet, String text) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getCell(0) != null) {
                String val = row.getCell(0).getStringCellValue();
                if (val.contains(text)) {
                    return row;
                }
            }
        }
        return null;
    }

    private Row findRowWithExactLabel(Sheet sheet, String label) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getCell(0) != null) {
                String val = row.getCell(0).getStringCellValue();
                if (val.equals(label)) {
                    return row;
                }
            }
        }
        return null;
    }
}
