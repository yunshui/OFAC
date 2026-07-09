package com.ofac.screening;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads names from an Excel file by locating a column header.
 */
public class ExcelNameReader {

    /**
     * Read non-empty string values from the specified column in the first sheet.
     *
     * @param filePath  path to the .xlsx file
     * @param columnName header text identifying the column
     * @return list of non-empty name strings
     */
    public static List<String> readNames(String filePath, String columnName) throws IOException {
        List<String> names = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("No sheets found in " + filePath);
            }

            int colIndex = findColumnIndex(sheet, columnName);
            if (colIndex < 0) {
                throw new IOException("Column '" + columnName + "' not found in sheet");
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell cell = row.getCell(colIndex);
                if (cell == null) continue;

                String val = getCellStringValue(cell).trim();
                if (!val.isEmpty()) {
                    names.add(val);
                }
            }
        }

        return names;
    }

    /**
     * Find the column index whose header (first row) matches the given name.
     */
    private static int findColumnIndex(Sheet sheet, String columnName) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return -1;

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null) {
                String header = getCellStringValue(cell).trim();
                if (header.equalsIgnoreCase(columnName)) {
                    return c;
                }
            }
        }
        return -1;
    }

    private static String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Avoid scientific notation for long numbers
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
