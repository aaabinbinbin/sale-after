---
name: order.query
version: 1.0.0
keywords: [订单, 买了, 购买, 收货, 发货, 物流, 我的订单]
tags: [order, query, read-only]
description: 查询订单基本信息、订单项和支付记录
roleRequired: none
dependsOn: []
requiredTools: [orderQueryTool]
riskLevel: LOW
requiresContext: order
---

# order.query

## 描述
查询指定订单的基本信息、订单项和支付记录。纯查询操作，不修改任何数据。
结果中包含每个订单项的售后状态和可退金额。

## 触发条件
- 用户提及订单号（如 O20xxx）
- 用户询问"我的订单"、"买了什么"、"订单状态"

## Prompt 模板
当前需要查询订单 {{orderNo}} 的信息。
已查到的订单数据：
{{toolResult}}

请根据这些信息回答用户的问题。如果用户是想申请售后，告知哪些商品可以申请售后。

## 约束
- 不能修改订单
- 不能创建售后单
- 不能推断不存在的订单信息
