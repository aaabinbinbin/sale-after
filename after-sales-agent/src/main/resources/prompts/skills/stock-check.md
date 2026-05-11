---
name: exchange.stock.check
version: 1.0.0
keywords: [换货, 库存, 有货吗, 能换吗, 换什么]
tags: [exchange, stock, check]
description: 检查换货目标 SKU 的库存是否足够，只检查不锁定
roleRequired: none
dependsOn: []
requiredTools: [stockCheckTool]
riskLevel: MEDIUM
requiresContext: stock
---

# exchange.stock.check

## 描述
检查指定 SKU 的当前可售库存（MySQL 库存 - Redis 锁定数）。
返回可用数量和是否满足换货需求。
只做库存检查，不锁定库存（锁定由业务服务完成）。

## 触发条件
- 用户询问"有没有货可以换"、"能换成XX吗"
- 换货场景中需要确认目标 SKU 库存

## Prompt 模板
库存检查结果：
{{toolResult}}

请告知用户目标商品是否有库存、是否满足换货需求。

## 约束
- 只检查，不锁定库存
- 库存数据以 MySQL + Redis 锁定数为准
