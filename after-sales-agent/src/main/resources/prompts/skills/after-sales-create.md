---
name: after_sales.application.create
version: 1.0.0
keywords: [创建售后, 提交申请]
tags: [after-sales, create, high-risk]
description: 创建真实售后单，属于高风险动作，必须用户确认
roleRequired: none
dependsOn: [after_sales.application.draft]
requiredTools: []
riskLevel: HIGH
requiresContext: order
---

# after_sales.application.create

## 描述
调用业务服务创建真实的售后单。
**高风险动作**：执行前必须生成 confirmToken 等待用户确认。
创建成功后更新订单项售后状态、记录操作日志。

## 触发条件
- 售后草稿已生成且用户已确认
- 幂等 Key 已就绪

## Prompt 模板
售后申请草稿已确认，正在创建售后单。
草稿内容：
{{contextData.draft}}

创建结果：{{toolResult}}

## 约束
- **必须经过 CONFIRM_REQUIRED 确认后才能执行**
- 必须通过状态机校验
- 必须校验幂等 Key
- 创建失败时明确告知原因
