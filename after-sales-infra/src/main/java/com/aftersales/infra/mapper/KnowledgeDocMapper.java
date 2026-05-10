package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeDocMapper {

    int insert(KnowledgeDoc doc);

    KnowledgeDoc selectByDocNo(@Param("docNo") String docNo);

    List<KnowledgeDoc> selectByType(@Param("docType") String docType);

    List<KnowledgeDoc> selectAll();

    /** 查询 RAG 评估集（带问题 + 期望文档） */
    List<Map<String, Object>> selectEvalDataset();
}
