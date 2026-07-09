package com.ofac.screening;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MatchClassifierTest {

    // ========== normalize() tests ==========

    @Test
    void normalizeReturnsEmptySetForNull() {
        assertTrue(MatchClassifier.normalize(null).isEmpty());
    }

    @Test
    void normalizeReturnsEmptySetForEmptyString() {
        assertTrue(MatchClassifier.normalize("").isEmpty());
    }

    @Test
    void normalizeReturnsEmptySetForBlankString() {
        assertTrue(MatchClassifier.normalize("   ").isEmpty());
    }

    @Test
    void normalizeLowercasesEnglishName() {
        Set<String> result = MatchClassifier.normalize("John DOE");
        assertTrue(result.contains("john"));
        assertTrue(result.contains("doe"));
        assertEquals(2, result.size());
    }

    @Test
    void normalizeStripsPunctuation() {
        Set<String> result = MatchClassifier.normalize("Chen, Ping!");
        assertTrue(result.contains("chen"));
        assertTrue(result.contains("ping"));
        assertEquals(2, result.size());
    }

    @Test
    void normalizeKeepsChineseCharacters() {
        Set<String> result = MatchClassifier.normalize("陈平");
        assertTrue(result.contains("陈平"));
        assertEquals(1, result.size());
    }

    @Test
    void normalizeHandlesMixedChineseAndEnglish() {
        Set<String> result = MatchClassifier.normalize("陈平 Chen, Ping");
        assertTrue(result.contains("陈平"));
        assertTrue(result.contains("chen"));
        assertTrue(result.contains("ping"));
        assertEquals(3, result.size());
    }

    @Test
    void normalizeTrimsAndDeduplicates() {
        Set<String> result = MatchClassifier.normalize("  John   JOHN  doe ");
        assertEquals(2, result.size());
        assertTrue(result.contains("john"));
        assertTrue(result.contains("doe"));
    }

    @Test
    void normalizeHandlesSpecialCharacters() {
        Set<String> result = MatchClassifier.normalize("O'Brien & Smith, Jr.");
        assertTrue(result.contains("obrien"));
        assertTrue(result.contains("smith"));
        assertTrue(result.contains("jr"));
        assertEquals(3, result.size());
    }

    // ========== classify() tests ==========

    @Test
    void classifyExactMatchChineseName() {
        // "陈平" vs ["陈平", "Chen, Ping"] → Exact Match (陈平 ⊆ 陈平)
        String result = MatchClassifier.classify("陈平", Arrays.asList("陈平", "Chen, Ping"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyExactMatchEnglishName() {
        // "John Doe" vs ["Jane Doe", "John Doe"] → Exact Match (john doe ⊆ john doe)
        String result = MatchClassifier.classify("John Doe", Arrays.asList("Jane Doe", "John Doe"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyExactMatchEnglishNameWithPunctuation() {
        // "Smith, John" → normalize → {"smith", "john"}
        // "John Smith" → normalize → {"john", "smith"}
        // {"john", "smith"} ⊆ {"smith", "john"} → Exact Match
        String result = MatchClassifier.classify("Smith, John", Arrays.asList("John Smith"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyExactMatchCaseInsensitive() {
        String result = MatchClassifier.classify("JOHN DOE", Arrays.asList("john doe"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyNotMatchCompletelyDifferentChinese() {
        // 张三 vs 李四 → Not Match (no shared words)
        String result = MatchClassifier.classify("张三", Arrays.asList("李四"));
        assertEquals("Not Match", result);
    }

    @Test
    void classifyNotMatchCompletelyDifferentEnglish() {
        String result = MatchClassifier.classify("John", Arrays.asList("Jane Smith"));
        assertEquals("Not Match", result);
    }

    @Test
    void classifyPartialMatchChineseAndEnglish() {
        // "John Smith" vs ["Smith, Jane"] → Partial Match (shared "smith" after normalization)
        String result = MatchClassifier.classify("John Smith", Arrays.asList("Smith, Jane"));
        assertEquals("Partial Match", result);
    }

    @Test
    void classifyPartialMatchWithMultipleNames() {
        // "John Smith" vs ["Jane Smith"] → Partial Match (shared "smith")
        String result = MatchClassifier.classify("John Smith", Arrays.asList("Jane Smith", "Bob Jones"));
        assertEquals("Partial Match", result);
    }

    @Test
    void classifyReturnsNotMatchForEmptyQueryName() {
        String result = MatchClassifier.classify("", Arrays.asList("John Doe"));
        assertEquals("Not Match", result);
    }

    @Test
    void classifyReturnsNotMatchForNullQueryName() {
        String result = MatchClassifier.classify(null, Arrays.asList("John Doe"));
        assertEquals("Not Match", result);
    }

    @Test
    void classifyReturnsNotMatchForEmptyHitList() {
        String result = MatchClassifier.classify("John", Collections.emptyList());
        assertEquals("Not Match", result);
    }

    @Test
    void classifyReturnsNotMatchForHitNamesWithNoOverlap() {
        // Query "AB" vs hit ["C D"] → punctuation-stripped: {"ab"} vs {"c", "d"} → Not Match
        String result = MatchClassifier.classify("A B", Arrays.asList("C D"));
        assertEquals("Not Match", result);
    }

    @Test
    void classifyPrefersExactOverPartial() {
        // Even if one hit is partial, if another hit is exact → Exact Match
        String result = MatchClassifier.classify("John Doe",
                Arrays.asList("Johnny Depp", "John Doe"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyHandlesNumbersInNames() {
        String result = MatchClassifier.classify("User123", Arrays.asList("User123 Test"));
        assertEquals("Exact Match", result);
    }

    @Test
    void classifyPartialWhenQueryIsSubsetOfHit() {
        String result = MatchClassifier.classify("Ping", Arrays.asList("Chen, Ping"));
        assertEquals("Exact Match", result);
    }
}
