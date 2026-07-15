package com.ofac.screening;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for raw OFAC blacklist API HTML response.
 */
@RestController
@RequestMapping("/searchRaw")
public class SearchRawController {

    private static final String DEFAULT_URL = "http://folcbla-asia.icbc:3012/";
    private static final String DEFAULT_USER = "cbla";
    private static final String DEFAULT_PASS = "Oper1234";
    private static final String DEFAULT_UNIT = "PEP00110";

    @GetMapping(produces = "text/html;charset=utf-8")
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
     * Basic HTML escaping for error messages.
     */
    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
