package com.ofac.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ofac.parser.model.HitDetail;
import com.ofac.parser.model.HitOverviewItem;
import com.ofac.parser.model.OFACQueryResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * OFAC HTML 页面解析工具。
 * <p>
 * 支持两种页面：
 * <ul>
 *   <li>No Hit — "OFAC-OnLine: No hit found"</li>
 *   <li>Hit(s) Detected — "OFAC-OnLine: Hit(s) detected"</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 *   OFACQueryResult result = OFACHtmlParser.parse(new File("response.html"));
 *   String json = OFACHtmlParser.toJson(result);
 *   System.out.println(json);
 * }</pre>
 */
public class OFACHtmlParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // ========== public API ==========

    /**
     * 解析 OFAC HTML 文件，返回结构化结果。
     */
    public static OFACQueryResult parse(File htmlFile) throws IOException {
        String html = new String(Files.readAllBytes(htmlFile.toPath()), StandardCharsets.UTF_8);
        return parse(html);
    }

    /**
     * 解析 OFAC HTML 字符串，返回结构化结果。
     */
    public static OFACQueryResult parse(String html) {
        Document doc = Jsoup.parse(html, Parser.htmlParser());
        String title = doc.title();

        if (title != null && title.contains("No hit found")) {
            return parseNoHit(doc, html);
        } else if (title != null && title.contains("Hit(s) detected")) {
            return parseHitsDetected(doc, html);
        } else {
            throw new IllegalArgumentException("Unrecognized OFAC page title: " + title);
        }
    }

    /**
     * 将结果序列化为格式化 JSON。
     */
    public static String toJson(OFACQueryResult result) {
        try {
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    // ========== No Hit 解析 ==========

    private static OFACQueryResult parseNoHit(Document doc, String html) {
        String fullText = doc.body() != null ? doc.body().text() : "";
        OFACQueryResult result = new OFACQueryResult(false, extractMessageId(fullText));
        result.setQueryName(extractQueryName(doc));
        extractMetadataTable(doc, result);
        return result;
    }

    // ========== Hit(s) Detected 解析 ==========

    private static OFACQueryResult parseHitsDetected(Document doc, String html) {
        String fullText = doc.body() != null ? doc.body().text() : "";
        OFACQueryResult result = new OFACQueryResult(true, extractMessageId(fullText));
        result.setTotalHits(extractTotalHits(fullText));
        result.setHitsOverview(extractOverviewTable(doc));
        result.setHitDetails(extractHitDetails(doc, html, fullText));
        extractMetadataTable(doc, result);
        return result;
    }

    // ========== 通用提取方法 ==========

    /**
     * 提取 "Message ID IS FOL-xxxxxxxxxxxxx"
     */
    static String extractMessageId(String text) {
        Pattern p = Pattern.compile("Message\\s*ID\\s*IS\\s*(\\S+)");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * 提取 "CASE NB HITS \d+"
     */
    static Integer extractTotalHits(String text) {
        Pattern p = Pattern.compile("CASE\\s+NB\\s+HITS\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { /* ignore */ }
        }
        return null;
    }

    // ========== Query Name ==========

    /**
     * "No Hit" 页面：在 "USER QUERY" 区域之后提取查询姓名
     */
    private static String extractQueryName(Document doc) {
        Elements bolds = doc.select("b, strong");
        for (Element b : bolds) {
            if (b.text().contains("USER QUERY")) {
                Element row = b.closest("tr");
                if (row != null) {
                    String rowText = row.text();
                    // 去掉 "USER QUERY" 前后的标签文字，剩下的就是查询姓名
                    String name = rowText.replaceAll("(?i)USER\\s*QUERY", "").trim();
                    if (!name.isEmpty()) return name;
                }
                // 回退：取下一个非空兄弟 cell 的文本
                Element td = b.closest("td");
                if (td != null) {
                    Element next = td.nextElementSibling();
                    if (next != null) {
                        String t = next.text().trim();
                        if (!t.isEmpty()) return t;
                    }
                }
            }
        }
        // 最后回退：在全文找 USER QUERY 之后的文字
        String bodyText = doc.body() != null ? doc.body().text() : "";
        Pattern p = Pattern.compile("USER\\s*QUERY\\s*([\\s\\S]{0,200}?)(?:List\\s+Date|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(bodyText);
        if (m.find()) {
            return m.group(1).replace('\u00A0', ' ').trim();
        }
        return null;
    }

    // ========== Metadata Table (No Hit 页面) ==========

    /**
     * 从 No Hit 页面的元数据表格中提取键值对
     */
    private static void extractMetadataTable(Document doc, OFACQueryResult result) {
        // 查找包含 "List Date" 的 tr
        Elements rows = doc.select("table tr");
        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.size() < 2) continue;
            String label = tds.get(0).text().replace('\u00A0', ' ').trim().toLowerCase();
            String value = tds.get(1).text().replace('\u00A0', ' ').trim();

            if (label.contains("list date"))          result.setListDate(value);
            else if (label.contains("list author"))   result.setListAuthor(value);
            else if (label.contains("list version"))  result.setListVersion(value);
            else if (label.contains("list title"))    result.setListTitle(value);
            else if (label.contains("list generated")) result.setListGeneratedWith(value);
            else if (label.contains("transaction id")) result.setTransactionId(value);
        }

        // 第二列（日期/作者/产品名）
        rows = doc.select("table tr");
        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.size() < 4) continue;
            String label1 = tds.get(2).text().replace('\u00A0', ' ').trim().toLowerCase();
            String value1 = tds.get(3).text().replace('\u00A0', ' ').trim();

            if (label1.contains("date"))             result.setDate(value1);
            else if (label1.contains("author"))      result.setAuthor(value1);
            else if (label1.contains("product name")) result.setProductName(value1);
            else if (label1.contains("product version")) result.setProductVersion(value1);
            else if (label1.contains("support email")) result.setSupportEmail(value1);
            else if (label1.contains("product copyright") || label1.contains("copyright")) result.setProductCopyright(value1);
        }
    }

    // ========== Quick Overview Table (Hit 页面) ==========

    /**
     * 从 raw HTML 中按 table 切分提取 Quick Overview 表格中的各行数据。
     * 列顺序：CONFID | ID | FIELD | ORIGIN | DESIGNATION | NAME | COUNTRY/REGION
     */
    private static List<HitOverviewItem> extractOverviewTable(Document doc) {
        List<HitOverviewItem> items = new ArrayList<>();
        Elements tables = doc.select("table");

        for (Element table : tables) {
            String tblText = table.text().toUpperCase().replace('\u00A0', ' ');
            // 确认这是概览表: 需要同时包含关键列名 CONFID, ORIGIN, DESIGNATION, NAME
            if (!tblText.contains("CONFID") || !tblText.contains("ORIGIN")
                    || !tblText.contains("DESIGNATION") || !tblText.contains("COUNTRY")) {
                continue;
            }
            // 排除 detail 表（含 HIT#、CATEGORIES、ADDRESS 等字段）
            if (tblText.contains("CATEGORIES") || tblText.contains("ADDRESS")) {
                continue;
            }

            Elements rows = table.select("tr");
            // 跳过标题/表头行，只提取数据行
            for (Element row : rows) {
                String rt = row.text().toUpperCase().replace('\u00A0', ' ').trim();
                if (rt.contains("CONFID") || rt.contains("WARNING") || rt.contains("MESSAGE ID")
                        || rt.contains("CASE NB") || rt.contains("HITS QUICK")
                        || rt.isEmpty()) {
                    continue;
                }
                Elements tds = row.select("td");
                if (tds.size() < 3) continue;

                // 检查第一个单元格是否为 CONFID 级别（如 50000%）或一段数字/特殊字符
                HitOverviewItem item = new HitOverviewItem();
                if (tds.size() >= 1) item.setConfidence(cleanCell(tds.get(0)));
                if (tds.size() >= 2) item.setId(cleanCell(tds.get(1)));
                if (tds.size() >= 3) item.setField(cleanCell(tds.get(2)));
                if (tds.size() >= 4) item.setOrigin(cleanCell(tds.get(3)));
                if (tds.size() >= 5) item.setDesignation(cleanCell(tds.get(4)));
                if (tds.size() >= 6) item.setName(cleanCell(tds.get(5)));
                if (tds.size() >= 7) item.setCountryRegion(cleanCell(tds.get(6)));

                if (item.getId() != null && !item.getId().isEmpty()) {
                    items.add(item);
                }
            }
            break; // 只处理第一个匹配的概览表
        }
        return items;
    }

    // ========== Hit Detail 提取 ==========

    /**
     * 提取每个 Hit 的详细信息。
     * 用 raw HTML regex 分割出每张 detail 表，然后 Jsoup 解析处理。
     */
    private static List<HitDetail> extractHitDetails(Document doc, String rawHtml, String fullText) {
        List<HitDetail> details = new ArrayList<>();

        // 在 raw HTML 中按 <table...HIT# 标记分割每一张 detail 表
        Pattern tablePattern = Pattern.compile(
                "<table[^>]*>.*?</table>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher tableMatcher = tablePattern.matcher(rawHtml);

        while (tableMatcher.find()) {
            String tblHtml = tableMatcher.group();
            if (!tblHtml.contains("HIT#")) continue;

            // 另一层确认：一定是 detail 表而不是概览表
            if (tblHtml.contains("CASE NB") || tblHtml.contains("HITS QUICK")) continue;

            // 用 Jsoup 解析这张独立的表
            Document tblDoc = Jsoup.parse(tblHtml, Parser.htmlParser());
            Element tbl = tblDoc.selectFirst("table");
            if (tbl == null) continue;

            HitDetail detail = parseSingleHitTable(tbl, tblHtml);
            if (detail != null && detail.getHitNumber() != null) {
                details.add(detail);
            }
        }

        details.sort(Comparator.comparing(HitDetail::getHitNumber));
        return details;
    }

    /**
     * 从整个页面的纯文本中提取一个标签字段（标签独占一行，值在下一行）
     */

    /**
     * 解析单个 Hit 的详情表格。
     * 使用 row-by-row 方式定位 label 行和 data 行。
     */
    private static HitDetail parseSingleHitTable(Element table, String tableHtml) {
        HitDetail detail = new HitDetail();
        String fullText = table.text().replace('\u00A0', ' ');

        // 1. HIT# number (使用 Jsoup 文本避免 HTML 标签干扰，格式: "HIT# 1", "HIT#15")
        Pattern hitNumPattern = Pattern.compile("HIT#\\s*(\\d+)");
        Matcher m = hitNumPattern.matcher(fullText);
        if (m.find()) {
            detail.setHitNumber(Integer.parseInt(m.group(1)));
        }

        // 2. ID 从 span[id] 获取
        Element hitIdSpan = table.selectFirst("span[id]");
        if (hitIdSpan != null) {
            detail.setId(hitIdSpan.text().trim());
        }

        // 3. 逐行解析 label-value 对
        Elements rows = table.select("tr");
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            String rowText = row.text().toUpperCase().replace('\u00A0', ' ').trim();
            boolean isHeader = row.hasAttr("style") && row.attr("style").contains("cccccc");

            // --- ID | ORIGIN | DESIGNATION | PRIORITY | CONFIDENTIALITY ---
            if (rowText.contains("ID") && rowText.contains("ORIGIN") && rowText.contains("DESIGNATION")
                    && !rowText.contains("NAME") && !rowText.contains("CATEGORIES") && !rowText.contains("SEARCHED")) {
                if (i + 1 < rows.size()) {
                    Elements dc = rows.get(i + 1).select("td");
                    if (dc.size() >= 1 && detail.getId() == null) detail.setId(cleanCell(dc.get(0)));
                    if (dc.size() >= 2) detail.setOrigin(cleanCell(dc.get(1)));
                    if (dc.size() >= 3) detail.setDesignation(cleanCell(dc.get(2)));
                    if (dc.size() >= 4) detail.setPriority(cleanCell(dc.get(3)));
                    if (dc.size() >= 5) detail.setConfidentiality(cleanCell(dc.get(4)));
                }
                continue;
            }

            // --- NAME | CITY | COUNTRY/REGION ---
            if (rowText.contains("NAME") && rowText.contains("CITY") && rowText.contains("COUNTRY")) {
                if (i + 1 < rows.size()) {
                    Elements dc = rows.get(i + 1).select("td");
                    if (!dc.isEmpty()) {
                        List<String> nameList = new ArrayList<>();
                        Element nameCell = dc.get(0);
                        for (Element p : nameCell.select("p, div, span, font")) {
                            String t = p.text().replace('\u00A0', ' ').trim();
                            if (!t.isEmpty() && !nameList.contains(t)) {
                                nameList.add(t);
                            }
                        }
                        if (nameList.isEmpty()) {
                            nameList.add(cleanCell(dc.get(0)));
                        }
                        detail.setNames(nameList);
                    }
                    if (dc.size() >= 2) detail.setCity(cleanCell(dc.get(1)));
                    if (dc.size() >= 3) {
                        List<String> countryList = new ArrayList<>();
                        Element countryCell = dc.get(2);
                        for (Element p : countryCell.select("p, div, span, font")) {
                            String t = p.text().replace('\u00A0', ' ').trim();
                            if (!t.isEmpty() && !countryList.contains(t)) {
                                countryList.add(t);
                            }
                        }
                        if (!countryList.isEmpty()) {
                            detail.setCountryRegion(countryList);
                        }
                    }
                }
                continue;
            }

            // --- SEARCHED CODES | BIC CODES | NATIONAL ID | PASSPORT NO ---
            if (rowText.contains("SEARCHED CODES") || rowText.contains("BIC CODES")
                    || (rowText.contains("NATIONAL ID") && !rowText.contains("SEARCHED"))) {
                if (i + 1 < rows.size()) {
                    Elements dc = rows.get(i + 1).select("td");
                    if (dc.size() >= 1) detail.setSearchedCodes(cleanCell(dc.get(0)));
                    if (dc.size() >= 2) detail.setBicCodes(cleanCell(dc.get(1)));
                    if (dc.size() >= 3) detail.setNationalId(cleanCell(dc.get(2)));
                    if (dc.size() >= 4) detail.setPassportNo(cleanCell(dc.get(3)));
                }
                continue;
            }

            // --- PLACE OF BIRTH | DATE OF BIRTH | USER INFO1 | USER INFO2 ---
            if (rowText.contains("PLACE OF BIRTH") || rowText.contains("DATE OF BIRTH")) {
                if (i + 1 < rows.size()) {
                    Elements dc = rows.get(i + 1).select("td");

                    // Determine which columns have data based on colspan
                    // The 4 columns may collapse into fewer <td> elements
                    String val1 = dc.size() >= 1 ? cleanCell(dc.get(0)) : null;
                    String val2 = dc.size() >= 2 ? cleanCell(dc.get(1)) : null;
                    String val3 = dc.size() >= 3 ? cleanCell(dc.get(2)) : null;
                    String val4 = dc.size() >= 4 ? cleanCell(dc.get(3)) : null;

                    // Map values to fields based on cell spans
                    if (val1 != null && isLikelyDate(val1)) detail.setDateOfBirth(val1);
                    else if (val1 != null) detail.setPlaceOfBirth(val1);

                    if (val2 != null) {
                        if (isLikelyDate(val2)) detail.setDateOfBirth(val2);
                        else if (val2.startsWith("Gender") || val2.contains(":")) detail.setUserInfo1(val2);
                        else detail.setUserInfo1(val2);
                    }
                    if (val3 != null) {
                        if (val3.startsWith("Gender") || val3.contains(":")) detail.setUserInfo1(val3);
                        else detail.setUserInfo2(val3);
                    }
                    if (val4 != null) detail.setUserInfo2(val4);
                }
                continue;
            }
        }

        // 4. 用正则从 fullText 中提取剩余字段（仅当 row 解析未设置时）
        // 注意：getter 可能返回 ""（nvl），用 isEmpty 判断
        if (detail.getCategories() == null || detail.getCategories().isEmpty()) detail.setCategories(extractValueBetween(fullText, "CATEGORIES", "KEYWORDS"));
        if (detail.getKeywords() == null || detail.getKeywords().isEmpty())   detail.setKeywords(  extractValueBetween(fullText, "KEYWORDS",   "TYPE"));
        if (detail.getType() == null || detail.getType().isEmpty())       detail.setType(      extractValueBetween(fullText, "TYPE",       "ADDRESS"));
        if (detail.getAddress() == null || detail.getAddress().isEmpty())    detail.setAddress(   extractValueBetween(fullText, "ADDRESS",    "SEARCHED CODES", "SEARCHED"));
        if (detail.getSearchedCodes() == null || detail.getSearchedCodes().isEmpty()) detail.setSearchedCodes(extractValueBetween(fullText, "SEARCHED CODES", "BIC CODES"));
        if (detail.getBicCodes() == null || detail.getBicCodes().isEmpty())   detail.setBicCodes(  extractValueBetween(fullText, "BIC CODES",  "NATIONAL ID"));
        if (detail.getNationalId() == null || detail.getNationalId().isEmpty()) detail.setNationalId(extractValueBetween(fullText, "NATIONAL ID","PASSPORT NO"));
        if (detail.getPassportNo() == null || detail.getPassportNo().isEmpty()) detail.setPassportNo(extractValueBetween(fullText, "PASSPORT NO","PLACE OF BIRTH"));
        // 注意: PLACE/DATE/USER INFO 依赖行解析, regex fallback 不可靠(Jsoup text顺序问题)
        detail.setOfficialReference(extractColonValue(fullText, "OFFICIAL REFERENCE", "ADDITIONAL INFO"));
        detail.setAdditionalInfo(   extractColonValue(fullText, "ADDITIONAL INFO", "RULE INFO"));
        detail.setRuleInfo(         extractColonValue(fullText, "RULE INFO",     null));

        return detail;
    }

    /**
     * 在文本中提取 label 与 nextLabel 之间的内容（label 可能无冒号，跨行匹配）。
     */
    private static String extractValueBetween(String text, String label, String... nextLabels) {
        String escaped = Pattern.quote(label);
        String nextPattern;
        if (nextLabels == null || nextLabels.length == 0 || nextLabels[0] == null) {
            nextPattern = "$";
        } else {
            nextPattern = Arrays.stream(nextLabels)
                    .filter(Objects::nonNull)
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
        }
        Pattern p = Pattern.compile(
                escaped + "\\s*:?\\s*(.*?)(?=" + nextPattern + "|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String val = m.group(1).replace('\u00A0', ' ').trim();
            val = val.replaceAll("\\s{2,}", " ").trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    /**
     * 提取 "LABEL: value" (带冒号的)。
     */
    private static String extractColonValue(String text, String label, String nextLabel) {
        String escaped = Pattern.quote(label);
        String next = nextLabel != null ? Pattern.quote(nextLabel) : "$";
        Pattern p = Pattern.compile(
                escaped + "\\s*:\\s*(.*?)(?=" + next + "|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String val = m.group(1).replace('\u00A0', ' ').trim();
            val = val.replaceAll("\\s{2,}", " ").trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }


    /**
     * 判断文本是否可能是日期（如 "Aug 1967", "Apr 1965 / Apr 1966"）
     */
    private static boolean isLikelyDate(String text) {
        return text != null && (
                text.matches("(?i)^[a-z]{3}\\s+\\d{4}.*")     // "Aug 1967"
                || text.matches("^\\d{4}.*")                   // "1967"
                || text.matches("^\\d{1,2}/\\d{1,2}/\\d{2,4}.*") // "01/01/2020"
        );
    }

    /**
     * 清洗单元格文本：替换 &nbsp;、去空白、去前后多余空格
     */
    private static String cleanCell(Element td) {
        String text = td.text().replace('\u00A0', ' ').trim();
        return text.isEmpty() ? null : text;
    }

    // ========== Main (示例 / 测试入口) ==========

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java OFACHtmlParser <html-file> [<html-file2> ...]");
            return;
        }

        for (String filePath : args) {
            File f = new File(filePath);
            if (!f.exists()) {
                System.err.println("File not found: " + filePath);
                continue;
            }
            try {
                OFACQueryResult result = parse(f);
                System.out.println(toJson(result));
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error parsing " + f.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
