package com.ofac.screening;

import com.ofac.parser.model.OFACQueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameResultTest {

    @Test
    void constructorWithQueryResult() {
        OFACQueryResult qr = new OFACQueryResult(true, "MSG1");
        NameResult nr = new NameResult("Test", qr);
        assertEquals("Test", nr.getQueryName());
        assertSame(qr, nr.getQueryResult());
        assertNull(nr.getError());
        assertFalse(nr.hasError());
        assertTrue(nr.isHitDetected());
    }

    @Test
    void constructorWithError() {
        NameResult nr = new NameResult("Test", "Something went wrong");
        assertEquals("Test", nr.getQueryName());
        assertNull(nr.getQueryResult());
        assertEquals("Something went wrong", nr.getError());
        assertTrue(nr.hasError());
        assertFalse(nr.isHitDetected());
    }

    @Test
    void constructorWithAllFields() {
        OFACQueryResult qr = new OFACQueryResult(false, "MSG2");
        NameResult nr = new NameResult("Test", qr, "Error detail");
        assertEquals("Test", nr.getQueryName());
        assertSame(qr, nr.getQueryResult());
        assertEquals("Error detail", nr.getError());
        assertTrue(nr.hasError());
        assertFalse(nr.isHitDetected());
    }

    @Test
    void isHitDetectedIsFalseWhenQueryResultIsNull() {
        NameResult nr = new NameResult("Test", "Error");
        assertFalse(nr.isHitDetected());
    }

    @Test
    void hasErrorIsFalseWhenErrorIsEmpty() {
        NameResult nr = new NameResult("Test", new OFACQueryResult(false, "M"), "");
        assertFalse(nr.hasError());
    }

    @Test
    void screeningResultsCollectsEntries() {
        ScreeningResults results = new ScreeningResults();
        assertEquals(0, results.size());

        results.add(new NameResult("A", "err"));
        results.add(new NameResult("B", new OFACQueryResult(false, "M")));

        assertEquals(2, results.size());
        assertEquals("A", results.getAll().get(0).getQueryName());
        assertEquals("B", results.getAll().get(1).getQueryName());
    }
}
