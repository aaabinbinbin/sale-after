package com.aftersales.rag.service;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.infra.mapper.KnowledgeDocMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识切片服务。
 *
 * 将知识文档按策略切分为多个 chunk：
 * - 政策/FAQ：按段落切分，每 chunk 300~800 字，overlap 50~100 字
 * - 案例：按 背景/诉求/处理/结果 结构组织
 */
@Service
public class KnowledgeChunkService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkService.class);

    // 简单切片：按双换行符（段落）切分，短段落合并，长段落再切
    private static final int TARGET_CHUNK_SIZE = 500;  // 目标切片大小（字符）
    private static final int MIN_CHUNK_SIZE = 100;      // 最小切片大小
    private static final int MAX_CHUNK_SIZE = 800;      // 最大切片大小
    private static final int OVERLAP_SIZE = 80;         // 重叠字符数

    private final KnowledgeDocMapper knowledgeDocMapper;

    public KnowledgeChunkService(KnowledgeDocMapper knowledgeDocMapper) {
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    /**
     * 对指定文档进行切片。
     *
     * @return 切片列表，每项包含 content、chunkIndex、tokenCount
     */
    public List<Map<String, Object>> chunkDocument(KnowledgeDoc doc) {
        String content = doc.getContent();
        if (content == null || content.isBlank()) return List.of();

        // 1. 按段落初步切分
        String[] paragraphs = content.split("\\n\\s*\\n");

        // 2. 合并短段落，切分长段落
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() <= MAX_CHUNK_SIZE) {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            } else {
                // 当前累积的 chunk 先保存
                if (current.length() >= MIN_CHUNK_SIZE) {
                    chunks.add(current.toString());
                }
                // 长段落单独处理
                if (para.length() > MAX_CHUNK_SIZE) {
                    chunks.addAll(splitLongParagraph(para));
                } else {
                    current = new StringBuilder(para);
                }
            }
        }
        if (current.length() >= MIN_CHUNK_SIZE) {
            chunks.add(current.toString());
        } else if (!chunks.isEmpty() && current.length() > 0) {
            // 短尾巴合并到上一个 chunk
            int last = chunks.size() - 1;
            chunks.set(last, chunks.get(last) + "\n\n" + current);
        }

        // 3. 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            Map<String, Object> chunk = new LinkedHashMap<>();
            chunk.put("chunkIndex", i + 1);
            chunk.put("content", chunkContent);
            chunk.put("tokenCount", estimateTokens(chunkContent));
            chunk.put("docId", doc.getId());
            chunk.put("docNo", doc.getDocNo());
            result.add(chunk);
        }

        log.info("文档切片完成 docNo={} chunkCount={}", doc.getDocNo(), result.size());
        return result;
    }

    /**
     * 长段落再切分（按句子边界，带 overlap）。
     */
    private List<String> splitLongParagraph(String para) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < para.length()) {
            int end = Math.min(start + TARGET_CHUNK_SIZE, para.length());
            // 尽量在句号/换行处切断
            if (end < para.length()) {
                int cutPoint = para.lastIndexOf('。', end);
                if (cutPoint > start + MIN_CHUNK_SIZE) end = cutPoint + 1;
                else {
                    cutPoint = para.lastIndexOf('\n', end);
                    if (cutPoint > start + MIN_CHUNK_SIZE) end = cutPoint + 1;
                }
            }
            result.add(para.substring(start, end).trim());
            start = end - OVERLAP_SIZE;
            if (start < 0) start = 0;
            if (start >= para.length() - MIN_CHUNK_SIZE) break;
        }
        return result;
    }

    /** 粗略 Token 估算：中文约 1.5 字符/token，英文约 4 字符/token */
    private int estimateTokens(String text) {
        int chineseChars = 0, otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) chineseChars++;
            else otherChars++;
        }
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }
}
