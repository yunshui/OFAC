package com.ofac.screening;

/**
 * Immutable configuration holder for the OFAC blacklist API connection.
 */
public class ApiConfig {

    private final String url;
    private final String user;
    private final String pass;
    private final String unit;

    public ApiConfig(String url, String user, String pass, String unit) {
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.unit = unit;
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
    public String getPass() { return pass; }
    public String getUnit() { return unit; }
}
