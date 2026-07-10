package com.ofac.screening;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads names from the first column of an Excel file, skipping the header row.
 */
public class ExcelNameReader {

    /**
     * Read non-empty string values from the first column (column 0) in the first sheet.
     * The first row is treated as a header and skipped.
     *
     * @param filePath path to the .xlsx file
     * @return list of non-empty name strings
     */
    public static List<String> readNames(String filePath) throws IOException {
        List<String> names = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("No sheets found in " + filePath);
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell cell = row.getCell(0);
                if (cell == null) continue;

                String val = getCellStringValue(cell).trim();
                if (!val.isEmpty()) {
                    names.add(val);
                }
            }
        }

        return names;
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
