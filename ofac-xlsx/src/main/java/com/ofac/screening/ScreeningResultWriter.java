package com.ofac.screening;

import com.ofac.parser.model.HitDetail;
import com.ofac.parser.model.OFACQueryResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes screening results to an Excel file with one sheet per queried name.
 * Each sheet contains metadata header rows followed by a detail table.
 */
public class ScreeningResultWriter {

    private static final String[] DETAIL_FIELDS = {
            "Match", "ID", "ORIGIN", "DESIGNATION", "PRIORITY", "CONFIDENTIALITY",
            "Names", "CITY", "COUNTRY/REGION",
            "CATEGORIES", "KEYWORDS", "TYPE", "ADDRESS",
            "SEARCHED CODES", "BIC CODES", "NATIONAL ID", "PASSPORT NO",
            "PLACE OF BIRTH", "DATE OF BIRTH", "USER INFO1", "USER INFO2",
            "OFFICIAL REFERENCE", "ADDITIONAL INFO", "RULE INFO"
    };

    private CellStyle boldStyle;
    private CellStyle greyFillStyle;
    private CellStyle wrapStyle;
    private CellStyle wrapBoldStyle;

    /**
     * Write screening results to an Excel workbook.
     *
     * @param results   the collected screening results
     * @param outputPath path to the output .xlsx file
     */
    public void write(ScreeningResults results, String outputPath) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            initStyles(wb);

            for (NameResult nr : results.getAll()) {
                String sheetName = sanitizeSheetName(nr.getQueryName());
                Sheet sheet = wb.createSheet(sheetName);

                if (nr.hasError()) {
                    writeErrorSheet(sheet, nr);
                } else if (!nr.isHitDetected()) {
                    writeNoHitSheet(sheet, nr);
                } else {
                    writeHitSheet(sheet, nr);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
    }

    private void initStyles(Workbook wb) {
        // Bold (used in metadata labels)
        boldStyle = wb.createCellStyle();
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        boldStyle.setWrapText(true);

        // Grey fill (used for detail table headers)
        greyFillStyle = wb.createCellStyle();
        Font greyBoldFont = wb.createFont();
        greyBoldFont.setBold(true);
        greyFillStyle.setFont(greyBoldFont);
        greyFillStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        greyFillStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greyFillStyle.setBorderTop(BorderStyle.THIN);
        greyFillStyle.setBorderBottom(BorderStyle.THIN);
        greyFillStyle.setBorderLeft(BorderStyle.THIN);
        greyFillStyle.setBorderRight(BorderStyle.THIN);
        greyFillStyle.setWrapText(true);

        // Wrap text (used for data cells)
        wrapStyle = wb.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setBorderTop(BorderStyle.THIN);
        wrapStyle.setBorderBottom(BorderStyle.THIN);
        wrapStyle.setBorderLeft(BorderStyle.THIN);
        wrapStyle.setBorderRight(BorderStyle.THIN);

        // Wrap + bold (used for field labels in detail table)
        wrapBoldStyle = wb.createCellStyle();
        Font wrapBoldFont = wb.createFont();
        wrapBoldFont.setBold(true);
        wrapBoldStyle.setFont(wrapBoldFont);
        wrapBoldStyle.setWrapText(true);
        wrapBoldStyle.setBorderTop(BorderStyle.THIN);
        wrapBoldStyle.setBorderBottom(BorderStyle.THIN);
        wrapBoldStyle.setBorderLeft(BorderStyle.THIN);
        wrapBoldStyle.setBorderRight(BorderStyle.THIN);
    }

    // ========== Error Sheet ==========

    private void writeErrorSheet(Sheet sheet, NameResult nr) {
        int rowNum = 0;
        rowNum = writeMetadataRow(sheet, rowNum, "Query Name", nr.getQueryName(), 3);
        rowNum = writeMetadataRow(sheet, rowNum, "Error", nr.getError(), 3);
        sheet.getRow(1).getCell(1).setCellStyle(wrapStyle);

        // No hits message
        Row msgRow = sheet.createRow(rowNum + 1);
        Cell msgCell = msgRow.createCell(0);
        msgCell.setCellValue("Error occurred during screening");
        msgCell.setCellStyle(boldStyle);

        autoSizeColumns(sheet, 3);
    }

    // ========== No Hit Sheet ==========

    private void writeNoHitSheet(Sheet sheet, NameResult nr) {
        OFACQueryResult r = nr.getQueryResult();
        int rowNum = 0;

        rowNum = writeMetadataRow(sheet, rowNum, "Query Name", nr.getQueryName(), 3);
        if (r != null) {
            rowNum = writeQueryMetadata(sheet, rowNum, r, 3);
        }

        Row msgRow = sheet.createRow(rowNum + 1);
        Cell msgCell = msgRow.createCell(0);
        msgCell.setCellValue("No hits found");
        msgCell.setCellStyle(boldStyle);

        autoSizeColumns(sheet, 3);
    }

    // ========== Hit Sheet ==========

    private void writeHitSheet(Sheet sheet, NameResult nr) {
        OFACQueryResult r = nr.getQueryResult();
        List<HitDetail> details = r.getHitDetails();
        if (details == null) details = new ArrayList<>();

        int numHits = details.size();

        // --- Metadata header rows (merged across all hit columns) ---
        int rowNum = 0;
        int metaCols = Math.max(numHits + 1, 4); // label col + hit cols; at least 4 for spacing

        rowNum = writeMetadataRow(sheet, rowNum, "Query Name", nr.getQueryName(), metaCols);
        if (r != null) {
            rowNum = writeQueryMetadata(sheet, rowNum, r, metaCols);
        }

        // --- Empty separator row ---
        rowNum++;

        // --- Detail table header: "Field \ Hit #" | Hit #1 | Hit #2 | ... ---
        int headerRowNum = rowNum;
        Row headerRow = sheet.createRow(rowNum);
        Cell hdrLabel = headerRow.createCell(0);
        hdrLabel.setCellValue("Field \\ Hit #");
        hdrLabel.setCellStyle(greyFillStyle);

        for (int i = 0; i < numHits; i++) {
            Cell hc = headerRow.createCell(i + 1);
            hc.setCellValue("Hit #" + details.get(i).getHitNumber());
            hc.setCellStyle(greyFillStyle);
        }
        rowNum++;

        // --- Detail rows: one row per field ---
        for (String field : DETAIL_FIELDS) {
            Row row = sheet.createRow(rowNum);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(field);
            labelCell.setCellStyle(wrapBoldStyle);

            for (int i = 0; i < numHits; i++) {
                Cell cell = row.createCell(i + 1);
                String value = getFieldValue(field, details.get(i), nr.getQueryName());
                cell.setCellValue(value);
                cell.setCellStyle(wrapStyle);
            }
            rowNum++;
        }

        autoSizeColumns(sheet, numHits + 1);
    }

    /**
     * Extract the value of a given field from a HitDetail.
     */
    private String getFieldValue(String field, HitDetail detail, String queryName) {
        if (field.equals("Match")) {
            String matchType = MatchClassifier.classify(queryName, detail.getNames());
            return matchType;
        }
        if (field.equals("Names")) {
            if (detail.getNames() == null || detail.getNames().isEmpty()) return "";
            return String.join("\n", detail.getNames());
        }
        if (field.equals("COUNTRY/REGION")) {
            if (detail.getCountryRegion() == null || detail.getCountryRegion().isEmpty()) return "";
            return String.join("\n", detail.getCountryRegion());
        }

        switch (field) {
            case "ID":               return detail.getId();
            case "ORIGIN":           return detail.getOrigin();
            case "DESIGNATION":      return detail.getDesignation();
            case "PRIORITY":         return detail.getPriority();
            case "CONFIDENTIALITY":  return detail.getConfidentiality();
            case "CITY":             return detail.getCity();
            case "CATEGORIES":       return detail.getCategories();
            case "KEYWORDS":         return detail.getKeywords();
            case "TYPE":             return detail.getType();
            case "ADDRESS":          return detail.getAddress();
            case "SEARCHED CODES":   return detail.getSearchedCodes();
            case "BIC CODES":        return detail.getBicCodes();
            case "NATIONAL ID":      return detail.getNationalId();
            case "PASSPORT NO":      return detail.getPassportNo();
            case "PLACE OF BIRTH":   return detail.getPlaceOfBirth();
            case "DATE OF BIRTH":    return detail.getDateOfBirth();
            case "USER INFO1":       return detail.getUserInfo1();
            case "USER INFO2":       return detail.getUserInfo2();
            case "OFFICIAL REFERENCE": return detail.getOfficialReference();
            case "ADDITIONAL INFO":  return detail.getAdditionalInfo();
            case "RULE INFO":        return detail.getRuleInfo();
            default:                 return "";
        }
    }

    // ========== Utility Methods ==========

    /**
     * Write a metadata row: label in column 0, value merged across columns 1..totalCols-1.
     */
    private int writeMetadataRow(Sheet sheet, int rowNum, String label, String value, int totalCols) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(boldStyle);

        // Merge columns 1..totalCols-1 for the value
        if (totalCols > 2) {
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, totalCols - 1));
        }

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(wrapStyle);

        return rowNum + 1;
    }

    /**
     * Write all available query metadata rows. Only non-null fields are written.
     *
     * @return updated rowNum
     */
    private int writeQueryMetadata(Sheet sheet, int rowNum, OFACQueryResult r, int totalCols) {
        rowNum = writeMetadataRow(sheet, rowNum, "Hit Detected", String.valueOf(r.isHitDetected()), totalCols);
        if (r.getTotalHits() != null) {
            rowNum = writeMetadataRow(sheet, rowNum, "Total Hits", String.valueOf(r.getTotalHits()), totalCols);
        }
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Message ID", r.getMessageId(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "List Date", r.getListDate(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "List Author", r.getListAuthor(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "List Version", r.getListVersion(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "List Title", r.getListTitle(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "List Generated With", r.getListGeneratedWith(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Transaction ID", r.getTransactionId(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Date", r.getDate(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Author", r.getAuthor(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Product Name", r.getProductName(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Product Version", r.getProductVersion(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Support Email", r.getSupportEmail(), totalCols);
        rowNum = writeOptionalMetadataRow(sheet, rowNum, "Product Copyright", r.getProductCopyright(), totalCols);
        return rowNum;
    }

    /**
     * Write a metadata row only if the value is non-null and non-empty.
     */
    private int writeOptionalMetadataRow(Sheet sheet, int rowNum, String label, String value, int totalCols) {
        if (value == null || value.isEmpty()) return rowNum;
        return writeMetadataRow(sheet, rowNum, label, value, totalCols);
    }

    /**
     * Auto-size columns up to a limit for readability.
     */
    private void autoSizeColumns(Sheet sheet, int maxCols) {
        for (int c = 0; c < maxCols; c++) {
            sheet.autoSizeColumn(c);
            // Cap column width at 60 characters
            if (sheet.getColumnWidth(c) > 60 * 256) {
                sheet.setColumnWidth(c, 60 * 256);
            }
        }
    }

    /**
     * Sanitize a name for use as a sheet name (Excel max 31 chars, no []:*?/\).
     */
    static String sanitizeSheetName(String name) {
        if (name == null) return "Unknown";
        String sanitized = name.replaceAll("[\\[\\]:*?/\\\\]", "_").trim();
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31).trim();
        }
        return sanitized.isEmpty() ? "Sheet" : sanitized;
    }
}
