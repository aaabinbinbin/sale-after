package com.aftersales.agent.loader;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/**
 * Skill 加载器。
 *
 * 启动时扫描 prompts/skills/*.md，解析 YAML 头部 + Markdown 正文为 SkillDefinition。
 * 提供版本校验、权限检查。当前规模（10 个）全量加载，预留 LRU 缓存接口。
 *
 * 设计模式：策略模式——不同文档结构可扩展不同的解析策略。
 */
@Component
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);

    /** 已加载的 Skill 定义缓存 */
    private final Map<String, SkillDefinition> cache = new ConcurrentHashMap<>();

    /** YAML 头部正则：匹配 --- ... --- */
    private static final Pattern YAML_HEADER = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);

    /**
     * 启动时扫描并加载全部 Skill md 文件。
     * 当前 Skill 数量少（10 个），全量加载足够。
     */
    @PostConstruct
    public void loadAll() {
        log.info("开始加载 Skill Markdown 文档...");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:prompts/skills/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                try {
                    SkillDefinition def = parseSkill(resource);
                    def.setLoadedAt(System.currentTimeMillis());
                    def.setValid(true);
                    // 使用 YAML 中声明的 name 作为 key（而非文件名），与 LLM 生成的 skill 名一致
                    String key = def.getName() != null ? def.getName() : filename.replace(".md", "");
                    cache.put(key, def);
                    log.info("Skill 加载成功: {} v{} (文件: {})", key, def.getVersion(), filename);
                } catch (Exception e) {
                    log.error("Skill 加载失败: {} - {}", filename, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Skill Markdown 目录扫描失败", e);
        }
        log.info("Skill 加载完成，共 {} 个", cache.size());
    }

    /**
     * 解析单个 Skill md 文件。
     *
     * @param resource md 文件资源
     * @return SkillDefinition
     */
    SkillDefinition parseSkill(Resource resource) throws IOException {
        String rawContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        SkillDefinition def = new SkillDefinition();

        // 1. 解析 YAML 头部
        Matcher matcher = YAML_HEADER.matcher(rawContent);
        if (matcher.find()) {
            String yamlBlock = matcher.group(1);
            parseYaml(yamlBlock, def);
        }

        // 2. 提取正文（YAML 头部之后的内容）
        String body = matcher.find() ? rawContent.substring(matcher.end()) : rawContent;
        // 提取 ## Prompt 模板 之后的文本作为 promptTemplate
        int promptStart = body.indexOf("## Prompt 模板");
        if (promptStart >= 0) {
            def.setPromptTemplate(body.substring(promptStart).trim());
        }

        // 3. 提取约束
        int constraintStart = body.indexOf("## 约束");
        if (constraintStart >= 0) {
            int nextSection = body.indexOf("##", constraintStart + 4);
            String constraintText = nextSection >= 0
                    ? body.substring(constraintStart, nextSection).trim()
                    : body.substring(constraintStart).trim();
            def.setConstraints(constraintText);
        }

        return def;
    }

    /**
     * 简易 YAML 行解析（不引入 SnakeYAML，当前字段足够简单）。
     */
    private void parseYaml(String yaml, SkillDefinition def) {
        String[] lines = yaml.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int colon = line.indexOf(':');
            if (colon < 0) continue;

            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();

            switch (key) {
                case "name" -> def.setName(value);
                case "version" -> def.setVersion(value);
                case "description" -> def.setDescription(value);
                case "roleRequired" -> def.setRoleRequired(value);
                case "riskLevel" -> def.setRiskLevel(value);
                case "requiresContext" -> def.setRequiresContext(value);
                case "keywords" -> def.setKeywords(parseList(value));
                case "tags" -> def.setTags(parseList(value));
                case "dependsOn" -> def.setDependsOn(parseList(value));
                case "requiredTools" -> def.setRequiredTools(parseList(value));
            }
        }
    }

    /** 解析 YAML 列表值：[a, b, c] */
    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) return List.of();
        // 去掉方括号
        String inner = value.replaceAll("[\\[\\]]", "").trim();
        if (inner.isEmpty()) return List.of();
        return Arrays.stream(inner.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 获取 Skill 定义（从缓存）。
     */
    public SkillDefinition get(String skillName) {
        SkillDefinition def = cache.get(skillName);
        if (def == null) {
            throw new BusinessException(ErrorCode.AGENT_SKILL_NOT_FOUND, "Skill 未找到: " + skillName);
        }
        return def;
    }

    /**
     * 带权限检查的获取。
     */
    public SkillDefinition getWithPermission(String skillName) {
        SkillDefinition def = get(skillName);
        String roleRequired = def.getRoleRequired();
        if (roleRequired != null && !"none".equalsIgnoreCase(roleRequired)) {
            String currentRole = UserContext.getRole();
            if (!roleRequired.equalsIgnoreCase(currentRole) && !"ADMIN".equalsIgnoreCase(currentRole)) {
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        "Skill " + skillName + " 需要 " + roleRequired + " 权限");
            }
        }
        return def;
    }

    /** 获取全部已加载的 Skill 名称 */
    public Set<String> allSkillNames() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /** 获取全部已加载的 Skill 定义 */
    public Collection<SkillDefinition> allSkills() {
        return Collections.unmodifiableCollection(cache.values());
    }

    /** 按名称批量获取 */
    public List<SkillDefinition> getBatch(List<String> names) {
        return names.stream().map(this::get).toList();
    }
}
