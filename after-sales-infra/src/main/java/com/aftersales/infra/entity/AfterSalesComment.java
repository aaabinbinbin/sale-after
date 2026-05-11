package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/** 售后协作评论实体 */
public class AfterSalesComment {
    private Long id;
    private Long afterSalesId;
    private String afterSalesNo;
    private Long commenterId;
    private String commenterRole;
    private String content;
    private Boolean internalOnly; // 是否仅内部可见
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }
    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }
    public Long getCommenterId() { return commenterId; }
    public void setCommenterId(Long commenterId) { this.commenterId = commenterId; }
    public String getCommenterRole() { return commenterRole; }
    public void setCommenterRole(String commenterRole) { this.commenterRole = commenterRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getInternalOnly() { return internalOnly; }
    public void setInternalOnly(Boolean internalOnly) { this.internalOnly = internalOnly; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
