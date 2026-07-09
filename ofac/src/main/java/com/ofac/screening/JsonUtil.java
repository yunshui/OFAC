package com.ofac.screening;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Utility for JSON serialization with empty2null support.
 */
public class JsonUtil {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private static final ObjectMapper NON_EMPTY_MAPPER = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /**
     * Serialize object to JSON string.
     *
     * @param obj        the object to serialize
     * @param empty2null if true, exclude empty string fields from output
     * @return formatted JSON string
     */
    public static String toJson(Object obj, boolean empty2null) {
        try {
            ObjectMapper mapper = empty2null ? NON_EMPTY_MAPPER : DEFAULT_MAPPER;
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
