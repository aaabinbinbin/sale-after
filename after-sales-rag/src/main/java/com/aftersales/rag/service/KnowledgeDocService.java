package com.aftersales.rag.service;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.infra.mapper.KnowledgeDocMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识文档服务。
 */
@Service
public class KnowledgeDocService {

    private final KnowledgeDocMapper knowledgeDocMapper;

    public KnowledgeDocService(KnowledgeDocMapper knowledgeDocMapper) {
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    public KnowledgeDoc create(String docType, String title, String sourceType, String sourceId, String content) {
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocNo(IdGenerator.genKnowledgeDocNo());
        doc.setDocType(docType);
        doc.setTitle(title);
        doc.setSourceType(sourceType);
        doc.setSourceId(sourceId);
        doc.setContent(content);
        doc.setStatus("ACTIVE");
        doc.setVersion(0L);
        knowledgeDocMapper.insert(doc);
        return doc;
    }

    public List<KnowledgeDoc> listByType(String docType) {
        return knowledgeDocMapper.selectByType(docType);
    }

    public List<KnowledgeDoc> listAll() {
        return knowledgeDocMapper.selectAll();
    }

    public KnowledgeDoc getByDocNo(String docNo) {
        return knowledgeDocMapper.selectByDocNo(docNo);
    }
}
