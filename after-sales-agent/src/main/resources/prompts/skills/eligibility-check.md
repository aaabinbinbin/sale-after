---
name: after_sales.eligibility.check
version: 1.0.0
keywords: [售后资格, 能不能退, 能不能换, 可不可以, 售后, 退货条件]
tags: [eligibility, check, after-sales]
description: 判断订单是否允许售后，校验数量、金额、窗口期、是否重复
roleRequired: none
dependsOn: [order.query]
requiredTools: []
riskLevel: LOW
requiresContext: eligibility_rules
---

# after_sales.eligibility.check

## 描述
根据订单状态、签收时间、订单项信息判断用户是否具备售后资格。
校验项：订单存在性、订单归属、订单状态、订单项归属、数量上限、金额上限、售后窗口期、是否已有售后处理中。

## 触发条件
- 用户询问"能不能退"、"可以换货吗"、"符不符合条件"
- 需要在创建售后申请之前确认资格

## Prompt 模板
售后资格校验结果：
{{toolResult}}

请根据校验结果告知用户是否满足售后条件。如果不满足，明确说明原因和可能的替代方案。

## 约束
- 只做资格判断，不创建售后单
- 资格不通过时，主动告知原因和替代方案
