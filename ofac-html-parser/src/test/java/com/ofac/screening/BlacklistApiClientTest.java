package com.ofac.screening;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistApiClientTest {

    private HttpServer server;
    private int port;
    private volatile String lastRequestBody;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        lastRequestBody = null;

        server.createContext("/", exchange -> {
            // Read request body
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            lastRequestBody = sb.toString();

            // Parse query params to check "name" field
            Map<String, String> params = parseFormBody(lastRequestBody);

            // Return success HTML response
            String htmlResponse = "<html><title>OFAC-OnLine: Hit(s) detected</title><body>"
                    + "<p>Query: " + params.getOrDefault("name", "") + "</p>"
                    + "<p>User: " + params.getOrDefault("user", "") + "</p>"
                    + "<p>Unit: " + params.getOrDefault("UNIT", "") + "</p>"
                    + "</body></html>";

            byte[] bytes = htmlResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void queryReturnsHtmlOnSuccess() {
        ApiConfig config = new ApiConfig("http://localhost:" + port + "/", "cbla", "Oper1234", "PEP00110");
        BlacklistApiClient client = new BlacklistApiClient(config);

        String result = client.query("John Doe");
        assertNotNull(result);
        assertTrue(result.contains("Hit(s) detected"));
        assertTrue(result.contains("Query: John Doe"));
    }

    @Test
    void querySendsCorrectFormBody() {
        ApiConfig config = new ApiConfig("http://localhost:" + port + "/", "testuser", "testpass", "TESTUNIT");
        BlacklistApiClient client = new BlacklistApiClient(config);

        client.query("Test Name");
        assertNotNull(lastRequestBody);

        Map<String, String> params = parseFormBody(lastRequestBody);
        assertEquals("Test Name", params.get("name"));
        assertEquals("testuser", params.get("user"));
        assertEquals("testpass", params.get("pass"));
        assertEquals("TESTUNIT", params.get("UNIT"));
        assertEquals("", params.get("ADDR"));
        assertEquals("", params.get("city"));
    }

    @Test
    void queryReturnsNullOnNon200Status() throws Exception {
        // Override the existing handler with one that returns 500
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] bytes = "Error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });

        ApiConfig config = new ApiConfig("http://localhost:" + port + "/", "cbla", "Oper1234", "PEP00110");
        BlacklistApiClient client = new BlacklistApiClient(config);

        String result = client.query("Any Name");
        assertNull(result);
    }

    @Test
    void queryReturnsNullOnConnectionFailure() {
        // Use a port that's not running
        ApiConfig config = new ApiConfig("http://localhost:1/", "cbla", "Oper1234", "PEP00110");
        BlacklistApiClient client = new BlacklistApiClient(config);

        String result = client.query("Any Name");
        assertNull(result);
    }

    @Test
    void queryHandlesChineseCharacters() {
        ApiConfig config = new ApiConfig("http://localhost:" + port + "/", "cbla", "Oper1234", "PEP00110");
        BlacklistApiClient client = new BlacklistApiClient(config);

        String result = client.query("陈平");
        assertNotNull(result);
        assertTrue(result.contains("陈平"));
    }

    @Test
    void querySendsCorrectContentType() {
        ApiConfig config = new ApiConfig("http://localhost:" + port + "/", "cbla", "Oper1234", "PEP00110");
        BlacklistApiClient client = new BlacklistApiClient(config);

        client.query("Test");
        assertNotNull(lastRequestBody);
        assertTrue(lastRequestBody.contains("name=Test"));
    }

    // ========== Helper ==========

    private Map<String, String> parseFormBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) return params;

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }
}
