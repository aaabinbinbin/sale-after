---
name: refund.estimate
version: 1.0.0
keywords: [退款预估, 退多少钱, 退款金额, 能退多少, 预估]
tags: [refund, estimate]
description: 根据订单信息预估可退款金额，只预估不执行
roleRequired: none
dependsOn: [order.query]
requiredTools: [orderQueryTool]
riskLevel: MEDIUM
requiresContext: order
---

# refund.estimate

## 描述
根据订单实付金额和订单项可退金额，预估最大可退款金额。
退款金额以订单实付为上限，每个订单项独立计算。
只做预估，不执行退款。

## 触发条件
- 用户询问"能退多少钱"、"退款金额是多少"

## Prompt 模板
订单信息：
{{toolResult}}

请告知用户最大可退款金额、各项明细、退款上限的计算依据。

## 约束
- 只做预估，不执行退款
- 金额必须基于订单实际数据，不能编造
