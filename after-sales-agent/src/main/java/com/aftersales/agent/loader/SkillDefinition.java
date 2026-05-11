package com.aftersales.agent.loader;

import java.util.*;

/**
 * Skill Markdown 文档解析后的定义对象。
 *
 * 每个实例对应 prompts/skills/ 下的一个 .md 文件。
 * 包含 YAML 头部元数据 + Markdown 正文（prompt 模板）。
 */
public class SkillDefinition {

    // ====== YAML 头部解析的元数据 ======

    private String name;            // md 文件名（不含 .md），如 "order-query"
    private String version;         // 版本号，如 "1.0.0"
    private List<String> keywords;  // 触发关键词
    private List<String> tags;      // 分类标签
    private String description;     // 一句话描述
    private String roleRequired;    // 需要的角色：none / CUSTOMER_SERVICE / ADMIN
    private List<String> dependsOn; // 依赖的其他 Skill 名称
    private List<String> requiredTools; // 需要的工具（映射到 ToolRegistry）
    private String riskLevel;       // 风险等级：LOW / MEDIUM / HIGH
    private String requiresContext; // 需要的数据来源，逗号分隔

    // ====== Markdown 正文 ======

    private String promptTemplate;  // Prompt 模板（含 {{variable}} 占位符）
    private String constraints;     // 约束条件
    private String outputFormat;    // 输出格式描述

    // ====== 运行时状态 ======

    private long loadedAt;          // 加载时间戳
    private boolean valid;          // 是否通过校验

    public SkillDefinition() {}

    // ====== getter / setter ======

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<String> getKeywords() { return keywords != null ? keywords : List.of(); }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getTags() { return tags != null ? tags : List.of(); }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }

    public List<String> getDependsOn() { return dependsOn != null ? dependsOn : List.of(); }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }

    public List<String> getRequiredTools() { return requiredTools != null ? requiredTools : List.of(); }
    public void setRequiredTools(List<String> requiredTools) { this.requiredTools = requiredTools; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getRequiresContext() { return requiresContext; }
    public void setRequiresContext(String requiresContext) { this.requiresContext = requiresContext; }

    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }

    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public long getLoadedAt() { return loadedAt; }
    public void setLoadedAt(long loadedAt) { this.loadedAt = loadedAt; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
}
