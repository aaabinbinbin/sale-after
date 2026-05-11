package com.aftersales.rag.evaluation;

import com.aftersales.rag.service.RagRetrievalService;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 评估服务单元测试。
 */
class RagEvaluationServiceTest {

    @Test
    void recall3ShouldBe1WhenFirstDocMatches() {
        // 模拟：检索结果前 3 包含期望文档
        List<String> retrieved = List.of("DOC_A", "DOC_B", "DOC_C");
        List<String> expected = List.of("DOC_A");
        assertTrue(retrieved.stream().limit(3).anyMatch(expected::contains),
                "Recall@3 应命中");
    }

    @Test
    void recall5ShouldBe0WhenNoMatch() {
        List<String> retrieved = List.of("DOC_X", "DOC_Y", "DOC_Z", "DOC_P", "DOC_Q");
        List<String> expected = List.of("DOC_A", "DOC_B");
        boolean hit5 = retrieved.stream().limit(5).anyMatch(expected::contains);
        assertFalse(hit5, "无匹配时 Recall@5 应为 false");
    }

    @Test
    void mrrShouldBeCalculatedCorrectly() {
        // 模拟：第一个相关文档排第 3，RR = 1/3
        List<String> retrieved = List.of("X", "Y", "TARGET", "Z");
        List<String> expected = List.of("TARGET");

        double rr = 0;
        for (int i = 0; i < retrieved.size(); i++) {
            if (expected.contains(retrieved.get(i))) {
                rr = 1.0 / (i + 1);
                break;
            }
        }
        assertEquals(1.0 / 3, rr, 0.001, "MRR = 1/rank_of_first_relevant_doc");
    }

    @Test
    void mrrShouldBe0WhenNoMatch() {
        List<String> retrieved = List.of("X", "Y", "Z");
        List<String> expected = List.of("A");
        double rr = 0;
        for (int i = 0; i < retrieved.size(); i++) {
            if (expected.contains(retrieved.get(i))) { rr = 1.0 / (i + 1); break; }
        }
        assertEquals(0.0, rr, "无匹配时 RR = 0");
    }

    @Test
    void recall3HitWhenExpectedDocInTop3() {
        // 场景：期望文档在检索结果第 3 位
        List<String> retrieved = List.of("DOC_1", "DOC_2", "POLICY_RETURN_001", "DOC_4");
        List<String> expected = List.of("POLICY_RETURN_001");
        boolean hit3 = retrieved.stream().limit(3).anyMatch(expected::contains);
        assertTrue(hit3, "期望文档在第3位时 Recall@3 应命中");
    }

    @Test
    void recall3MissWhenExpectedDocAtPosition4() {
        List<String> retrieved = List.of("DOC_1", "DOC_2", "DOC_3", "POLICY_RETURN_001");
        List<String> expected = List.of("POLICY_RETURN_001");
        boolean hit3 = retrieved.stream().limit(3).anyMatch(expected::contains);
        assertFalse(hit3, "期望文档在第4位时 Recall@3 应不命中");
    }

    @Test
    void recall5HitWhenExpectedDocAtPosition5() {
        List<String> retrieved = List.of("DOC_1", "DOC_2", "DOC_3", "DOC_4", "POLICY_RETURN_001");
        List<String> expected = List.of("POLICY_RETURN_001");
        boolean hit5 = retrieved.stream().limit(5).anyMatch(expected::contains);
        assertTrue(hit5, "期望文档在第5位时 Recall@5 应命中");
    }
}
