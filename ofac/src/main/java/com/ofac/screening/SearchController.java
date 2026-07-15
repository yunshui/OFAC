package com.ofac.screening;

import com.ofac.parser.OFACHtmlParser;
import com.ofac.parser.model.OFACQueryResult;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OFAC blacklist search.
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    private static final String DEFAULT_URL = "http://folcbla-asia.icbc:3012/";
    private static final String DEFAULT_USER = "cbla";
    private static final String DEFAULT_PASS = "Oper1234";
    private static final String DEFAULT_UNIT = "PEP00110";

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE + ";charset=utf-8")
    public ResponseEntity<String> search(
            @RequestParam("name") String name,
            @RequestParam(value = "empty2null", defaultValue = "true") boolean empty2null,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "pass", required = false) String pass,
            @RequestParam(value = "unit", required = false) String unit) {

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"name parameter is required\"}");
        }

        String apiUrl = url != null ? url : DEFAULT_URL;
        String apiUser = user != null ? user : DEFAULT_USER;
        String apiPass = pass != null ? pass : DEFAULT_PASS;
        String apiUnit = unit != null ? unit : DEFAULT_UNIT;

        ApiConfig config = new ApiConfig(apiUrl, apiUser, apiPass, apiUnit);
        BlacklistApiClient client = new BlacklistApiClient(config);

        String html = client.query(name);
        if (html == null) {
            return ResponseEntity.status(502)
                    .body("{\"error\": \"API query failed for name: " + escapeJson(name) + "\"}");
        }

        try {
            OFACQueryResult result = OFACHtmlParser.parse(html);
            String json = JsonUtil.toJson(result, empty2null);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body("{\"error\": \"Failed to parse API response: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Query the OFAC blacklist API and return raw HTML response (unparsed).
     */
    @GetMapping(value = "/raw", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> searchRaw(
            @RequestParam("name") String name,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "pass", required = false) String pass,
            @RequestParam(value = "unit", required = false) String unit) {

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("<error>name parameter is required</error>");
        }

        String apiUrl = url != null ? url : DEFAULT_URL;
        String apiUser = user != null ? user : DEFAULT_USER;
        String apiPass = pass != null ? pass : DEFAULT_PASS;
        String apiUnit = unit != null ? unit : DEFAULT_UNIT;

        ApiConfig config = new ApiConfig(apiUrl, apiUser, apiPass, apiUnit);
        BlacklistApiClient client = new BlacklistApiClient(config);

        String html = client.query(name);
        if (html == null) {
            return ResponseEntity.status(502)
                    .body("<error>API query failed for name: " + escapeHtml(name) + "</error>");
        }

        return ResponseEntity.ok(html);
    }

    /**
     * Basic JSON string escaping for error messages.
     */
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Basic HTML escaping for error messages.
     */
    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
