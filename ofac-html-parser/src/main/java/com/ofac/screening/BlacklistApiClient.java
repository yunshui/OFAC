package com.ofac.screening;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for the OFAC blacklist API.
 * Uses java.net.HttpURLConnection (no extra dependencies).
 * Port of query_blacklist_api() from request_script.py.
 */
public class BlacklistApiClient {

    private final ApiConfig config;

    public BlacklistApiClient(ApiConfig config) {
        this.config = config;
    }

    /**
     * Query the blacklist API for the given name.
     *
     * @param name the name to search
     * @return raw HTML response string, or null on failure
     */
    public String query(String name) {
        try {
            URL url = new URL(config.getUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Origin", "null");
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            String body = buildFormBody(name);
            conn.setRequestProperty("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString().trim();
            } else {
                System.err.println("  [Query Failed] Status " + status + ": " + name);
                return null;
            }
        } catch (IOException e) {
            System.err.println("  [Network Error] " + e.getMessage());
            return null;
        }
    }

    private String buildFormBody(String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "name", name);
        appendParam(sb, "ADDR", "");
        appendParam(sb, "city", "");
        appendParam(sb, "STAT", "");
        appendParam(sb, "CTRY", "");
        appendParam(sb, "CODE", "");
        appendParam(sb, "BIC", "");
        appendParam(sb, "NID", "");
        appendParam(sb, "PSP", "");
        appendParam(sb, "TYPE", "");
        appendParam(sb, "DATEOFBIRTH", "");
        appendParam(sb, "UNIT", config.getUnit());
        appendParam(sb, "user", config.getUser());
        appendParam(sb, "pass", config.getPass());
        // Remove trailing &
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) throws IOException {
        sb.append(URLEncoder.encode(key, "UTF-8"));
        sb.append("=");
        sb.append(URLEncoder.encode(value, "UTF-8"));
        sb.append("&");
    }
}
