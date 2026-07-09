package com.ofac.screening;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiConfigTest {

    @Test
    void constructorSetsAllFields() {
        ApiConfig config = new ApiConfig("http://test.url/", "user1", "pass1", "UNIT1");
        assertEquals("http://test.url/", config.getUrl());
        assertEquals("user1", config.getUser());
        assertEquals("pass1", config.getPass());
        assertEquals("UNIT1", config.getUnit());
    }

    @Test
    void handlesEmptyValues() {
        ApiConfig config = new ApiConfig("", "", "", "");
        assertEquals("", config.getUrl());
        assertEquals("", config.getUser());
        assertEquals("", config.getPass());
        assertEquals("", config.getUnit());
    }

    @Test
    void handlesNullValues() {
        ApiConfig config = new ApiConfig(null, null, null, null);
        assertNull(config.getUrl());
        assertNull(config.getUser());
        assertNull(config.getPass());
        assertNull(config.getUnit());
    }
}
