package com.ofac.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 顶层查询结果，包含页面元数据 + hit列表（如有）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"hitDetected", "messageId", "queryName", "totalHits",
        "listDate", "listAuthor", "listVersion", "listTitle",
        "listGeneratedWith", "transactionId", "date", "author",
        "productName", "productVersion", "supportEmail", "productCopyright",
        "hitsOverview", "hitDetails"})
public class OFACQueryResult {

    private boolean hitDetected;
    private String messageId;
    private String queryName;
    private Integer totalHits;

    // ---- metadata (No Hit 页面和 Hit 页面都可能有) ----
    private String listDate;
    private String listAuthor;
    private String listVersion;
    private String listTitle;
    private String listGeneratedWith;
    private String transactionId;
    private String date;
    private String author;
    private String productName;
    private String productVersion;
    private String supportEmail;
    private String productCopyright;

    // ---- Hit 页面 ----
    private List<HitOverviewItem> hitsOverview;
    private List<HitDetail> hitDetails;

    // ---- constructors ----
    public OFACQueryResult() {}

    public OFACQueryResult(boolean hitDetected, String messageId) {
        this.hitDetected = hitDetected;
        this.messageId = messageId;
    }

    // ---- getters / setters ----
    public boolean isHitDetected() { return hitDetected; }
    public void setHitDetected(boolean hitDetected) { this.hitDetected = hitDetected; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }

    public Integer getTotalHits() { return totalHits; }
    public void setTotalHits(Integer totalHits) { this.totalHits = totalHits; }

    public String getListDate() { return listDate; }
    public void setListDate(String listDate) { this.listDate = listDate; }

    public String getListAuthor() { return listAuthor; }
    public void setListAuthor(String listAuthor) { this.listAuthor = listAuthor; }

    public String getListVersion() { return listVersion; }
    public void setListVersion(String listVersion) { this.listVersion = listVersion; }

    public String getListTitle() { return listTitle; }
    public void setListTitle(String listTitle) { this.listTitle = listTitle; }

    public String getListGeneratedWith() { return listGeneratedWith; }
    public void setListGeneratedWith(String listGeneratedWith) { this.listGeneratedWith = listGeneratedWith; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getProductCopyright() { return productCopyright; }
    public void setProductCopyright(String productCopyright) { this.productCopyright = productCopyright; }

    public List<HitOverviewItem> getHitsOverview() { return hitsOverview; }
    public void setHitsOverview(List<HitOverviewItem> hitsOverview) { this.hitsOverview = hitsOverview; }

    public List<HitDetail> getHitDetails() { return hitDetails; }
    public void setHitDetails(List<HitDetail> hitDetails) { this.hitDetails = hitDetails; }
}
