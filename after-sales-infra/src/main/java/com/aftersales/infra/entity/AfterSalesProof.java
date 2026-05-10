package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/**
 * 售后凭证实体。
 * file_key 存对象存储 key，不存本机绝对路径。
 */
public class AfterSalesProof {
    private Long id;
    private Long afterSalesId;
    private String afterSalesNo;
    private String proofType; // IMAGE/VIDEO/PDF/OTHER
    private String fileName;
    private String fileKey; // 对象存储文件Key
    private String contentType;
    private Long fileSize;
    private Long uploaderId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }
    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }
    public String getProofType() { return proofType; }
    public void setProofType(String proofType) { this.proofType = proofType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
