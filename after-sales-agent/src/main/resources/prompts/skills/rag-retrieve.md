---
name: rag.retrieve
version: 1.0.0
keywords: [政策, 规则, 规定, 案例, 历史, 以前, 类似]
tags: [rag, retrieve, knowledge]
description: 从知识库检索相关政策、FAQ、历史案例
roleRequired: none
dependsOn: []
requiredTools: [ragRetrieveTool]
riskLevel: LOW
requiresContext: rag
---

# rag.retrieve

## 描述
从售后知识库（政策文档、FAQ、历史案例、客服手册）中检索相关内容。
返回最相关的知识片段及引用来源。
只在需要知识支撑时调用，不做每次强制检索。

## 触发条件
- 用户询问售后政策/规则
- Agent 不确定处理规则，需要查政策
- 客服需要参考历史案例

## Prompt 模板
检索问题：{{userInput}}

检索结果：
{{toolResult}}

请根据检索到的知识回答用户问题，并引用来源。

## 约束
- 不替代订单查询和状态机
- 检索结果仅供参考，不直接作为决策依据
