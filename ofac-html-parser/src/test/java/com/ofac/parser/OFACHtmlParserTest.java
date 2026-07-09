package com.ofac.parser;

import com.ofac.parser.model.HitDetail;
import com.ofac.parser.model.HitOverviewItem;
import com.ofac.parser.model.OFACQueryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OFACHtmlParserTest {

    private static final String BASE_DIR = "/Users/yunshuiyang/Workspace/OFAC/";

    @Test
    void parseNoHit() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ No hit found1-陈大文.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        assertFalse(result.isHitDetected());
        assertEquals("FOL-20260708-111550-000001", result.getMessageId());
        assertEquals("陈大文", result.getQueryName());

        // Metadata fields
        assertEquals("2026-07-07 10:26:09", result.getListDate());
        assertEquals("LYODS", result.getListAuthor());
        assertEquals("ICBC China List Update - Full", result.getListTitle());
        assertEquals("FOFLIST [WIN32]", result.getListGeneratedWith());
        assertEquals("FOL-20260708-111550-000001", result.getTransactionId());
        assertEquals("2026/07/08-11:15:50", result.getDate());
        assertEquals("cbla", result.getAuthor());
        assertEquals("OFAC-Agent OnLine", result.getProductName());
        assertEquals("MailTo:Support@FircoSoft.com", result.getSupportEmail());

        // No hit fields should be null
        assertNull(result.getTotalHits());
        assertNull(result.getHitsOverview());
        assertNull(result.getHitDetails());
    }

    @Test
    void parseHitDetected() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        assertTrue(result.isHitDetected());
        assertEquals("FOL-20260708-111249-000001", result.getMessageId());
        assertEquals(Integer.valueOf(15), result.getTotalHits());
    }

    @Test
    void overviewTableHas15Items() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        assertEquals(15, result.getHitsOverview().size());
    }

    @Test
    void overviewFieldsCorrect() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        HitOverviewItem first = result.getHitsOverview().get(0);
        assertEquals("50000%", first.getConfidence());
        assertEquals("FA1013100", first.getId());
        assertEquals("NAM", first.getField());
        assertEquals("FACTIVA", first.getOrigin());
        assertEquals("OTHER", first.getDesignation());
        assertEquals("Chen, Ping", first.getName());
        assertEquals("Taiwan", first.getCountryRegion());

        HitOverviewItem item3 = result.getHitsOverview().get(2);
        assertEquals("CORRUPTION", item3.getDesignation());

        HitOverviewItem last = result.getHitsOverview().get(14);
        assertEquals("FA4779733", last.getId());
    }

    @Test
    void hitDetailNumbers() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        assertEquals(1, result.getHitDetails().get(0).getHitNumber().intValue());
        assertEquals(2, result.getHitDetails().get(1).getHitNumber().intValue());
        assertEquals(15, result.getHitDetails().get(2).getHitNumber().intValue());
    }

    @Test
    void hitDetailIdsCorrect() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        assertEquals("FA1013100", result.getHitDetails().get(0).getId());
        assertEquals("FA11415311", result.getHitDetails().get(1).getId());
        assertEquals("FA4779733", result.getHitDetails().get(2).getId());
    }

    @Test
    void hitDetailHeaderFields() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        HitDetail d1 = result.getHitDetails().get(0);
        assertEquals("FACTIVA", d1.getOrigin());
        assertEquals("OTHER", d1.getDesignation());
        assertEquals("0", d1.getPriority());
        assertEquals("50000%", d1.getConfidentiality());
    }

    @Test
    void hitDetailNamesDeduplicated() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        HitDetail d1 = result.getHitDetails().get(0);
        assertTrue(d1.getNames().size() > 0);
        // Verify no duplicates
        assertEquals(d1.getNames().size(), d1.getNames().stream().distinct().count());
        assertTrue(d1.getNames().contains("陈平"));
        assertTrue(d1.getNames().contains("Chen, Ping"));
    }

    @Test
    void hitDetailTypeAndCategory() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        for (HitDetail d : result.getHitDetails()) {
            assertEquals("Individual", d.getType());
            assertTrue(d.getCategories().contains("Politically Exposed Person"));
        }
    }

    @Test
    void hitDetailPersonalInfo() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        // HIT#1 (陈平 - Taiwan, Gender:F)
        HitDetail d1 = result.getHitDetails().get(0);
        assertEquals(Arrays.asList("Taiwan"), d1.getCountryRegion());
        assertEquals("Gender:F", d1.getUserInfo1());
        assertEquals("", d1.getDateOfBirth());   // null → ""
        assertEquals("", d1.getUserInfo2());      // null → ""

        // HIT#2 (陈平 - China PEP, Gender:M, Aug 1967)
        HitDetail d2 = result.getHitDetails().get(1);
        assertEquals("Aug 1967", d2.getDateOfBirth());
        assertEquals("Gender:M", d2.getUserInfo1());
        assertTrue(d2.getCountryRegion().contains("China"));
        assertTrue(d2.getCountryRegion().contains("PRC"));
        assertTrue(d2.getCountryRegion().contains("中国"));

        // HIT#15 (陈平 - China PEP, Gender:M, Apr 1965 / Apr 1966)
        HitDetail d3 = result.getHitDetails().get(2);
        assertEquals("Apr 1965 / Apr 1966", d3.getDateOfBirth());
        assertEquals("Gender:M", d3.getUserInfo1());
    }

    @Test
    void hitDetailOfficialReference() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(BASE_DIR + "OFAC-OnLine_ Hit(s) detected1-陈平.html")));
        OFACQueryResult result = OFACHtmlParser.parse(html);

        for (HitDetail d : result.getHitDetails()) {
            assertTrue(d.getOfficialReference().contains("FACTIVA_"));
            assertNotNull(d.getAdditionalInfo());
            assertNotNull(d.getRuleInfo());
        }
    }
}
