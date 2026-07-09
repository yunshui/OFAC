package com.ofac.parser.model;

import java.util.List;

/**
 * 单个 Hit 的详细信息
 */
public class HitDetail {

    private Integer hitNumber;
    private String id;
    private String origin;
    private String designation;
    private String priority;
    private String confidentiality;

    // NAME (多个别名)
    private List<String> names;
    private String city;
    private List<String> countryRegion;

    private String categories;
    private String keywords;
    private String type;
    private String address;

    private String searchedCodes;
    private String bicCodes;
    private String nationalId;
    private String passportNo;

    private String placeOfBirth;
    private String dateOfBirth;
    private String userInfo1;
    private String userInfo2;

    private String officialReference;
    private String additionalInfo;
    private String ruleInfo;

    public HitDetail() {}

    public Integer getHitNumber() { return hitNumber; }
    public void setHitNumber(Integer hitNumber) { this.hitNumber = hitNumber; }

    public String getId() { return nvl(id); }
    public void setId(String id) { this.id = id; }

    public String getOrigin() { return nvl(origin); }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDesignation() { return nvl(designation); }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPriority() { return nvl(priority); }
    public void setPriority(String priority) { this.priority = priority; }

    public String getConfidentiality() { return nvl(confidentiality); }
    public void setConfidentiality(String confidentiality) { this.confidentiality = confidentiality; }

    public List<String> getNames() { return names; }
    public void setNames(List<String> names) { this.names = names; }

    public String getCity() { return nvl(city); }
    public void setCity(String city) { this.city = city; }

    public List<String> getCountryRegion() { return countryRegion; }
    public void setCountryRegion(List<String> countryRegion) { this.countryRegion = countryRegion; }

    public String getCategories() { return nvl(categories); }
    public void setCategories(String categories) { this.categories = categories; }

    public String getKeywords() { return nvl(keywords); }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getType() { return nvl(type); }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return nvl(address); }
    public void setAddress(String address) { this.address = address; }

    public String getSearchedCodes() { return nvl(searchedCodes); }
    public void setSearchedCodes(String searchedCodes) { this.searchedCodes = searchedCodes; }

    public String getBicCodes() { return nvl(bicCodes); }
    public void setBicCodes(String bicCodes) { this.bicCodes = bicCodes; }

    public String getNationalId() { return nvl(nationalId); }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getPassportNo() { return nvl(passportNo); }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }

    public String getPlaceOfBirth() { return nvl(placeOfBirth); }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }

    public String getDateOfBirth() { return nvl(dateOfBirth); }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getUserInfo1() { return nvl(userInfo1); }
    public void setUserInfo1(String userInfo1) { this.userInfo1 = userInfo1; }

    public String getUserInfo2() { return nvl(userInfo2); }
    public void setUserInfo2(String userInfo2) { this.userInfo2 = userInfo2; }

    public String getOfficialReference() { return nvl(officialReference); }
    public void setOfficialReference(String officialReference) { this.officialReference = officialReference; }

    public String getAdditionalInfo() { return nvl(additionalInfo); }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getRuleInfo() { return nvl(ruleInfo); }
    public void setRuleInfo(String ruleInfo) { this.ruleInfo = ruleInfo; }

    /**
     * null → "" 转换
     */
    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
