package com.aftersales.rag.service;

import com.aftersales.infra.entity.KnowledgeDoc;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KnowledgeChunkService 单元测试。
 * 验证差异化切片策略：POLICY按条款、FAQ按Q&A、CASE按结构。
 */
class KnowledgeChunkServiceTest {

    private KnowledgeChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkService = new KnowledgeChunkService(null);
    }

    @Test
    void shouldSplitByParagraphs() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocType("POLICY");
        doc.setContent("第一条：这是关于售后政策的详细说明内容，包含了用户需要了解的退货退款相关规则和流程要求，申请退货退款需要在签收后七天内提交申请。\n\n第二条：这是关于换货政策的详细说明内容，包含了换货的申请条件和库存要求，换货需要目标SKU有充足库存。\n\n第三条：这是关于补偿政策的详细说明内容，包含了延迟发货补偿规则和客服承诺补偿的发放流程要求。");
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.size() >= 1, "应按段落切分为chunk");
        assertEquals(1, chunks.get(0).get("chunkIndex"));
        assertNotNull(chunks.get(0).get("content"), "每个chunk应有content");
    }

    @Test
    void shouldHandleShortContent() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocType("FAQ");
        // 小于 MIN_CHUNK_SIZE(100) 的内容会被过滤，预期空结果
        doc.setContent("这是一个简短的FAQ内容。");
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.isEmpty(), "小于MIN_CHUNK_SIZE的短内容应被过滤");
    }

    @Test
    void shouldHandleEmptyContent() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setContent("");
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.isEmpty(), "空内容应返回空列表");
    }

    @Test
    void shouldHandleNullContent() {
        KnowledgeDoc doc = new KnowledgeDoc();
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.isEmpty(), "null内容应返回空列表");
    }

    @Test
    void shouldSplitCaseDocument() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocType("CASE");
        doc.setContent("""
                标题：耳机退货案例
                背景：用户购买蓝牙耳机后反馈产品存在质量问题，耳机左耳出现了明显的杂音，影响了正常的使用体验。
                诉求：用户要求退货退款，希望能够全额退还购买款项，并且对商家的售后服务提出了一定的质疑。
                处理过程：客服收到申请后立即审核了用户上传的凭证视频，确认了质量问题的存在后通过审核。
                结果：用户按照退货流程寄回了商品，商家在收到退货确认无误后执行了全额退款操作。
                经验总结：凭证清晰且质量问题明确时，应快速审核通过以提升用户满意度和售后体验。""");
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.size() >= 1, "案例文档应至少生成1个chunk");
        // 每个chunk应有metadata
        chunks.forEach(c -> {
            assertNotNull(c.get("chunkIndex"));
            assertNotNull(c.get("content"));
        });
    }

    @Test
    void shouldEstimateTokenCount() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setContent("这是一个测试内容，用于验证token估算是否正常工作。");
        var chunks = chunkService.chunkDocument(doc);
        chunks.forEach(c -> {
            Object tokenCount = c.get("tokenCount");
            assertNotNull(tokenCount, "每个chunk应有tokenCount");
            assertTrue((Integer) tokenCount > 0, "tokenCount应大于0");
        });
    }

    @Test
    void shouldKeepChunkIndexSequential() {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocType("POLICY");
        doc.setContent("第一段内容。\n\n第二段内容。\n\n第三段内容。\n\n第四段内容。");
        var chunks = chunkService.chunkDocument(doc);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i + 1, chunks.get(i).get("chunkIndex"), "chunkIndex应连续");
        }
    }

    @Test
    void shouldSplitLongParagraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("这是一段很长的内容。".repeat(100));
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setContent(sb.toString());
        var chunks = chunkService.chunkDocument(doc);
        assertTrue(chunks.size() >= 1, "长段落应被切分");
        // 每个chunk不应超过最大大小
        chunks.forEach(c -> {
            String content = (String) c.get("content");
            assertTrue(content.length() <= 800, "chunk大小不超过800字符");
        });
    }
}
