package com.ofac.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 快速概览表中的一行
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HitOverviewItem {

    private String confidence;
    private String id;
    private String field;
    private String origin;
    private String designation;
    private String name;
    private String countryRegion;

    public HitOverviewItem() {}

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountryRegion() { return countryRegion; }
    public void setCountryRegion(String countryRegion) { this.countryRegion = countryRegion; }
}
