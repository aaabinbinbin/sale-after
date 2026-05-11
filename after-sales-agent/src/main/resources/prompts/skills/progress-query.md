---
name: after_sales.progress.query
version: 1.0.0
keywords: [售后进度, 退款进度, 到哪了, 处理中, 状态, 售后状态]
tags: [after-sales, query, read-only]
description: 查询售后处理进度和当前状态
roleRequired: none
dependsOn: []
requiredTools: [afterSalesProgressTool]
riskLevel: LOW
requiresContext: after_sales
---

# after_sales.progress.query

## 描述
查询指定售后单的处理进度。返回当前状态、已完成的步骤、剩余步骤。
纯查询操作，不修改数据。

## 触发条件
- 用户询问"售后怎么样了"、"退款到哪了"、"处理完了吗"
- 上下文中已有 afterSalesNo

## Prompt 模板
售后单 {{afterSalesNo}} 的当前进度：
{{toolResult}}

请用通俗的语言告知用户售后处理到哪一步了，接下来还需要做什么。

## 约束
- 只查询不修改
- 不能编造不存在的进度信息
