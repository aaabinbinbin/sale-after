package com.aftersales.agent.registry;

import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.loader.SkillLoader;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillRegistry 单元测试。
 * 用 Mock SkillLoader 注入预定义的 Skill，验证分层匹配逻辑。
 */
class SkillRegistryTest {

    private SkillRegistry registry;
    private TestSkillLoader testLoader;

    @BeforeEach
    void setUp() {
        testLoader = new TestSkillLoader();
        testLoader.addSkill(buildSkill("order.query",
                List.of("订单", "买了", "收货"), List.of("order", "query")));
        testLoader.addSkill(buildSkill("after_sales.eligibility.check",
                List.of("能不能退", "售后资格"), List.of("eligibility", "check")));
        testLoader.addSkill(buildSkill("rag.retrieve",
                List.of("政策", "规则"), List.of("rag", "retrieve")));
        testLoader.addSkill(buildSkill("after_sales.application.draft",
                List.of("申请售后", "我要退"), List.of("after-sales", "draft", "application")));
        testLoader.addSkill(buildSkill("after_sales.progress.query",
                List.of("进度", "到哪了"), List.of("after-sales", "query")));
        registry = new SkillRegistry(testLoader);
    }

    @Test
    void shouldMatchByKeyword() {
        var matched = registry.keywordMatch("我要退货退款");
        assertFalse(matched.isEmpty(), "关键词匹配应命中");
        assertTrue(matched.get(0).getName().contains("application"),
                "匹配到的最相关 skill 应是 draft");
    }

    @Test
    void shouldMatchByKeywordForPolicy() {
        var matched = registry.keywordMatch("退货政策是什么");
        assertFalse(matched.isEmpty(), "政策关键词应命中");
        assertTrue(matched.stream().anyMatch(s -> s.getName().equals("rag.retrieve")),
                "应匹配到 rag.retrieve");
    }

    @Test
    void shouldReturnEmptyWhenNoKeywordMatch() {
        var matched = registry.keywordMatch("hello world");
        assertTrue(matched.isEmpty(), "无关键词匹配时应返回空列表");
    }

    @Test
    void shouldMatchByTagForEligibilityIntent() {
        // Tag 匹配：intent 拆分后的词与 skill.tags 匹配
        var matched = registry.tagMatch("ORDER_AFTER_SALES_ELIGIBILITY");
        assertFalse(matched.isEmpty(), "Tag 匹配应命中");
        assertTrue(matched.stream().anyMatch(s -> s.getTags().contains("eligibility")),
                "应匹配到 eligibility check skill");
    }

    @Test
    void shouldMatchByTagForCreateIntent() {
        var matched = registry.tagMatch("CREATE_AFTER_SALES_APPLICATION");
        assertFalse(matched.isEmpty(), "Tag 匹配应命中");
    }

    @Test
    void shouldReturnEmptyWhenNoTagMatch() {
        var matched = registry.tagMatch("UNKNOWN");
        assertTrue(matched.isEmpty(), "UNKNOWN intent 不应匹配到任何 tag");
    }

    @Test
    void shouldFallbackToAllSkillsWhenNoMatch() {
        // match 方法在前两层都失败时应降级返回非 HIGH 风险 skill
        var matched = registry.match("UNKNOWN", "xyz abc");
        assertFalse(matched.isEmpty(), "降级应返回可用 skill");
        matched.forEach(s -> assertNotEquals("HIGH", s.getRiskLevel(), "降级不应返回 HIGH 风险 skill"));
    }

    // ====== helper ======

    static SkillDefinition buildSkill(String name, List<String> keywords, List<String> tags) {
        SkillDefinition def = new SkillDefinition();
        def.setName(name);
        def.setKeywords(keywords);
        def.setTags(tags);
        def.setRiskLevel("LOW");
        def.setVersion("1.0.0");
        return def;
    }

    /** 测试用 SkillLoader，直接注入 Skill 列表 */
    static class TestSkillLoader extends SkillLoader {
        private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();
        void addSkill(SkillDefinition def) { skills.put(def.getName(), def); }
        @Override public SkillDefinition get(String name) { return skills.get(name); }
        @Override public Collection<SkillDefinition> allSkills() { return skills.values(); }
    }
}
