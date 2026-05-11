---
name: after_sales.application.draft
version: 1.0.0
keywords: [申请售后, 我要退, 我要换, 退款申请, 起草, 草稿]
tags: [after-sales, draft, create]
description: 生成售后申请草稿，把用户自然语言转成结构化申请
roleRequired: none
dependsOn: [order.query, after_sales.eligibility.check]
requiredTools: []
riskLevel: MEDIUM
requiresContext: order
---

# after_sales.application.draft

## 描述
根据订单信息和用户诉求，生成结构化的售后申请草稿。
草稿不创建真实售后单，仅供用户确认。
根据上下文自动推断售后类型（仅退款/退货退款/换货/补偿）。

## 触发条件
- 用户明确表示要发起售后
- 售后资格校验已通过

## Prompt 模板
订单信息：
{{contextData.order}}

用户诉求：{{userInput}}

请生成售后申请草稿，包含：
1. 建议的售后类型（REFUND_ONLY / RETURN_REFUND / EXCHANGE / COMPENSATION）
2. 申请原因
3. 涉及的商品清单（orderItemId、数量、金额）
4. 需要用户补充的信息（如凭证、物流等）

## 约束
- 只生成草稿，不创建真实售后单
- 售后类型必须与用户诉求匹配
- 金额不能超过可退金额
