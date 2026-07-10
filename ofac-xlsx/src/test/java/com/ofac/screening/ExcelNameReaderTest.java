package com.ofac.screening;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelNameReaderTest {

    private File testFile;

    @BeforeEach
    void setUp() throws Exception {
        testFile = File.createTempFile("ofac-reader-test-", ".xlsx");
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(testFile)) {
            Sheet sheet = wb.createSheet("Names");

            // Header row (row 0)
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("English Name");
            header.createCell(1).setCellValue("Other");

            // Data rows starting from row 1
            sheet.createRow(1).createCell(0).setCellValue("John Doe");
            sheet.createRow(2).createCell(0).setCellValue("Jane Smith");
            sheet.createRow(3).createCell(0).setCellValue("陈平");
            sheet.createRow(4).createCell(0).setCellValue("");  // empty cell
            sheet.createRow(5).createCell(0).setCellValue("  "); // blank cell
            sheet.createRow(6).createCell(0).setCellValue("Alice Wang");
            // Row 7 with numeric value
            Row row7 = sheet.createRow(7);
            Cell numCell = row7.createCell(0);
            numCell.setCellValue(12345);
            CellStyle style = wb.createCellStyle();
            style.setDataFormat(wb.createDataFormat().getFormat("@"));
            numCell.setCellStyle(style);

            wb.write(fos);
        }
    }

    @AfterEach
    void tearDown() {
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    void readNamesReturnsAllNonEmptyValues() throws Exception {
        List<String> names = ExcelNameReader.readNames(testFile.getAbsolutePath());
        assertEquals(5, names.size());
        assertTrue(names.contains("John Doe"));
        assertTrue(names.contains("Jane Smith"));
        assertTrue(names.contains("陈平"));
        assertTrue(names.contains("Alice Wang"));
        assertTrue(names.contains("12345"));
    }

    @Test
    void readNamesExcludesHeaderRow() throws Exception {
        List<String> names = ExcelNameReader.readNames(testFile.getAbsolutePath());
        assertFalse(names.contains("English Name"));
    }

    @Test
    void readNamesExcludesEmptyCells() throws Exception {
        List<String> names = ExcelNameReader.readNames(testFile.getAbsolutePath());
        assertFalse(names.contains(""));
        assertFalse(names.contains("  "));
    }

    @Test
    void readNamesThrowsForNonExistentFile() {
        assertThrows(IOException.class,
                () -> ExcelNameReader.readNames("/nonexistent/path.xlsx"));
    }

    @Test
    void readNamesReturnsEmptyForHeaderOnly() throws Exception {
        // Create a file with only a header row, no data rows
        File emptyFile = File.createTempFile("ofac-empty-", ".xlsx");
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(emptyFile)) {
            Sheet sheet = wb.createSheet("Empty");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("English Name");
            wb.write(fos);
        }

        try {
            List<String> names = ExcelNameReader.readNames(emptyFile.getAbsolutePath());
            assertTrue(names.isEmpty());
        } finally {
            emptyFile.delete();
        }
    }

    @Test
    void readNamesReadsFromSecondRow() throws Exception {
        // Verify that row 0 (header) is skipped and row 1 is the first data row
        File f = File.createTempFile("ofac-header-", ".xlsx");
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(f)) {
            Sheet sheet = wb.createSheet("Test");
            sheet.createRow(0).createCell(0).setCellValue("Header");
            sheet.createRow(1).createCell(0).setCellValue("FirstData");
            wb.write(fos);
        }

        try {
            List<String> names = ExcelNameReader.readNames(f.getAbsolutePath());
            assertEquals(1, names.size());
            assertEquals("FirstData", names.get(0));
        } finally {
            f.delete();
        }
    }
}
