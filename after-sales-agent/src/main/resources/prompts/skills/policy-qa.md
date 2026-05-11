---
name: policy.qa
version: 1.0.0
keywords: [售后政策, 退货规则, 换货规则, 退款规则, 质保, 保修]
tags: [policy, qa, knowledge]
description: 回答售后政策相关问题，基于 RAG 检索结果生成答案
roleRequired: none
dependsOn: [rag.retrieve]
requiredTools: []
riskLevel: LOW
requiresContext: rag
---

# policy.qa

## 描述
根据 RAG 检索到的政策内容，用自然语言回答用户的售后政策问题。
回答应包含政策依据和引用来源。

## 触发条件
- 用户明确询问售后政策
- 意图为 AFTER_SALES_POLICY_QA

## Prompt 模板
用户问题：{{userInput}}

检索到的相关政策：
{{contextData.ragResults}}

请基于以上政策内容回答用户问题。如果政策内容不足以回答，请诚实告知。

## 约束
- 回答必须基于检索到的政策内容，不能编造
- 引用具体的政策条款
- 不能替代订单查询和状态机判断
