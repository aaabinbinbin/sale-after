---
name: compensation.suggest
version: 1.0.0
keywords: [补偿, 赔偿, 优惠券, 积分, 安抚]
tags: [compensation, suggest]
description: 根据订单金额给出补偿建议（类型+金额），不执行
roleRequired: CUSTOMER_SERVICE
dependsOn: [order.query]
requiredTools: [orderQueryTool]
riskLevel: MEDIUM
requiresContext: order
---

# compensation.suggest

## 描述
根据订单实付金额计算补偿上限（10%），给出补偿类型和金额建议。
补偿类型包括优惠券、积分、余额、人工补偿。
只给建议，不执行发放。

## 触发条件
- 售后无法通过退款/退货解决，需要补偿
- 客服处理投诉时需要补偿建议

## Prompt 模板
订单信息：
{{toolResult}}

用户诉求：{{userInput}}

请给出补偿建议：
1. 补偿上限（订单实付的10%）
2. 建议的补偿类型（COUPON/POINTS/BALANCE/MANUAL）
3. 建议金额
4. 理由

## 约束
- 只给建议，不执行发放
- 金额不超过订单实付的10%
- 需要客服角色权限
