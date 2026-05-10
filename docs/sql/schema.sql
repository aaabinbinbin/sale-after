-- ============================================================================
-- 电商售后智能 Agent 平台 - 数据库建表脚本
--
-- 包含全部 25 张核心表：
--   业务基础表：user_account, product, sku, sku_stock, trade_order, trade_order_item, payment_record
--   售后核心表：after_sales_order, after_sales_item, after_sales_proof, after_sales_comment, after_sales_operation_log
--   售后履约表：refund_record, return_record, exchange_record, compensation_record
--   工程可靠性表：idempotency_record
--   Agent 表：agent_trace, agent_tool_call, agent_confirm_action
--   RAG 表：knowledge_doc, knowledge_chunk, knowledge_build_task, rag_eval_dataset, rag_eval_result
-- ============================================================================

CREATE DATABASE IF NOT EXISTS after_sales_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE after_sales_agent;

-- ============================================================================
-- 1. 用户账户表
-- ============================================================================
CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    role VARCHAR(32) NOT NULL COMMENT '角色：USER/CUSTOMER_SERVICE/ADMIN',
    phone VARCHAR(32) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_role_status (role, status)
) COMMENT='用户账户表';

-- ============================================================================
-- 2. 商品表
-- ============================================================================
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_name VARCHAR(255) NOT NULL COMMENT '商品名称',
    category_id BIGINT COMMENT '类目ID',
    brand_name VARCHAR(128) COMMENT '品牌名称',
    status VARCHAR(32) NOT NULL COMMENT '商品状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) COMMENT='商品表';

-- ============================================================================
-- 3. SKU 表
-- ============================================================================
CREATE TABLE sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    sku_name VARCHAR(255) NOT NULL COMMENT 'SKU名称',
    sale_price DECIMAL(18,2) NOT NULL COMMENT '销售价',
    status VARCHAR(32) NOT NULL COMMENT 'SKU状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id),
    KEY idx_status (status)
) COMMENT='SKU表';

-- ============================================================================
-- 4. SKU 库存表
-- ============================================================================
CREATE TABLE sku_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    total_stock INT NOT NULL COMMENT '总库存',
    available_stock INT NOT NULL COMMENT '可售库存',
    sold_stock INT NOT NULL COMMENT '已售库存',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_sku_id (sku_id)
) COMMENT='SKU库存表';

-- ============================================================================
-- 5. 交易订单表
-- ============================================================================
CREATE TABLE trade_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '订单总金额',
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    paid_amount DECIMAL(18,2) NOT NULL COMMENT '实付金额（订单快照）',
    paid_at DATETIME COMMENT '支付时间',
    shipped_at DATETIME COMMENT '发货时间',
    finished_at DATETIME COMMENT '完成时间',
    closed_at DATETIME COMMENT '关闭时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_status_created_at (order_status, created_at)
) COMMENT='交易订单表';

-- ============================================================================
-- 6. 交易订单项表
-- ============================================================================
CREATE TABLE trade_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_name VARCHAR(255) NOT NULL COMMENT '下单时商品名称快照',
    sku_name VARCHAR(255) NOT NULL COMMENT '下单时SKU名称快照',
    quantity INT NOT NULL COMMENT '购买数量',
    unit_price DECIMAL(18,2) NOT NULL COMMENT '下单单价',
    paid_amount DECIMAL(18,2) NOT NULL COMMENT '该订单项实付金额',
    refundable_amount DECIMAL(18,2) NOT NULL COMMENT '当前可退金额',
    after_sales_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '售后状态：NONE/PROCESSING/DONE',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_sku_id (sku_id),
    KEY idx_after_sales_status (after_sales_status)
) COMMENT='交易订单项表';

-- ============================================================================
-- 7. 支付记录表
-- ============================================================================
CREATE TABLE payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
    pay_amount DECIMAL(18,2) NOT NULL COMMENT '支付金额',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    paid_at DATETIME COMMENT '支付完成时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_pay_status_created_at (pay_status, created_at)
) COMMENT='支付记录表';

-- ============================================================================
-- 8. 售后主单表
-- ============================================================================
CREATE TABLE after_sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    after_sales_type VARCHAR(32) NOT NULL COMMENT '售后类型：REFUND_ONLY/RETURN_REFUND/EXCHANGE/COMPENSATION',
    status VARCHAR(64) NOT NULL COMMENT '售后状态',
    reason_code VARCHAR(64) COMMENT '售后原因编码',
    reason_text VARCHAR(1024) COMMENT '售后原因描述',
    apply_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '申请金额',
    approved_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '审核通过金额',
    applicant_remark VARCHAR(2048) COMMENT '申请人备注',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_remark VARCHAR(2048) COMMENT '审核备注',
    reviewed_at DATETIME COMMENT '审核时间',
    completed_at DATETIME COMMENT '完成时间',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_after_sales_no (after_sales_no),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_status_created_at (status, created_at),
    KEY idx_type_status_created_at (after_sales_type, status, created_at)
) COMMENT='售后主单表';

-- ============================================================================
-- 9. 售后明细项表
-- ============================================================================
CREATE TABLE after_sales_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    order_item_id BIGINT NOT NULL COMMENT '订单项ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_name VARCHAR(255) NOT NULL COMMENT '商品名称快照',
    sku_name VARCHAR(255) NOT NULL COMMENT 'SKU名称快照',
    apply_quantity INT NOT NULL COMMENT '申请售后数量',
    approved_quantity INT NOT NULL DEFAULT 0 COMMENT '审核通过数量',
    refundable_amount DECIMAL(18,2) NOT NULL COMMENT '可退金额',
    apply_amount DECIMAL(18,2) NOT NULL COMMENT '申请金额',
    approved_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '审核通过金额',
    exchange_sku_id BIGINT COMMENT '换货目标SKU ID',
    item_status VARCHAR(64) NOT NULL COMMENT '明细项状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no (after_sales_no),
    KEY idx_order_item_id (order_item_id),
    KEY idx_sku_id (sku_id)
) COMMENT='售后明细项表';

-- ============================================================================
-- 10. 售后凭证表
-- ============================================================================
CREATE TABLE after_sales_proof (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    after_sales_id BIGINT COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) COMMENT '售后单号',
    proof_type VARCHAR(32) NOT NULL COMMENT '凭证类型：IMAGE/VIDEO/PDF/OTHER',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_key VARCHAR(512) NOT NULL COMMENT '对象存储文件Key',
    content_type VARCHAR(128) COMMENT 'MIME类型',
    file_size BIGINT COMMENT '文件大小',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_uploader_id_created_at (uploader_id, created_at)
) COMMENT='售后凭证表';

-- ============================================================================
-- 11. 售后协作评论表
-- ============================================================================
CREATE TABLE after_sales_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    commenter_id BIGINT NOT NULL COMMENT '评论人ID',
    commenter_role VARCHAR(32) NOT NULL COMMENT '评论人角色',
    content TEXT NOT NULL COMMENT '评论内容',
    internal_only TINYINT NOT NULL DEFAULT 0 COMMENT '是否仅内部可见',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_after_sales_id_created_at (after_sales_id, created_at),
    KEY idx_after_sales_no_created_at (after_sales_no, created_at)
) COMMENT='售后协作评论表';

-- ============================================================================
-- 12. 售后操作日志表
-- ============================================================================
CREATE TABLE after_sales_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    operator_id BIGINT COMMENT '操作人ID',
    operator_role VARCHAR(32) COMMENT '操作人角色',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    from_status VARCHAR(64) COMMENT '操作前状态',
    to_status VARCHAR(64) COMMENT '操作后状态',
    operation_detail TEXT COMMENT '操作详情JSON或文本',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no_created_at (after_sales_no, created_at),
    KEY idx_operator_id_created_at (operator_id, created_at),
    KEY idx_operation_type_created_at (operation_type, created_at)
) COMMENT='售后操作日志表';

-- ============================================================================
-- 13. 退款记录表
-- ============================================================================
CREATE TABLE refund_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    payment_no VARCHAR(64) COMMENT '原支付流水号',
    refund_amount DECIMAL(18,2) NOT NULL COMMENT '退款金额',
    refund_status VARCHAR(32) NOT NULL COMMENT '退款状态',
    refund_channel VARCHAR(32) COMMENT '退款渠道',
    external_refund_no VARCHAR(128) COMMENT '外部退款流水号',
    failure_reason VARCHAR(1024) COMMENT '失败原因',
    refunded_at DATETIME COMMENT '退款成功时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no (after_sales_no),
    KEY idx_order_id (order_id),
    KEY idx_status_created_at (refund_status, created_at)
) COMMENT='退款记录表';

-- ============================================================================
-- 14. 退货记录表
-- ============================================================================
CREATE TABLE return_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    return_no VARCHAR(64) NOT NULL COMMENT '退货单号',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    logistics_company VARCHAR(128) COMMENT '退货物流公司',
    logistics_no VARCHAR(128) COMMENT '退货物流单号',
    return_status VARCHAR(32) NOT NULL COMMENT '退货状态',
    shipped_at DATETIME COMMENT '用户寄出时间',
    received_at DATETIME COMMENT '商家收货时间',
    receiver_id BIGINT COMMENT '收货确认人ID',
    receiver_remark VARCHAR(1024) COMMENT '收货备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_return_no (return_no),
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no (after_sales_no),
    KEY idx_logistics_no (logistics_no),
    KEY idx_status_created_at (return_status, created_at)
) COMMENT='退货记录表';

-- ============================================================================
-- 15. 换货记录表
-- ============================================================================
CREATE TABLE exchange_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    exchange_no VARCHAR(64) NOT NULL COMMENT '换货单号',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    exchange_status VARCHAR(32) NOT NULL COMMENT '换货状态',
    outbound_logistics_company VARCHAR(128) COMMENT '换货发出物流公司',
    outbound_logistics_no VARCHAR(128) COMMENT '换货发出物流单号',
    stock_locked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已锁定库存',
    shipped_at DATETIME COMMENT '换货发出时间',
    completed_at DATETIME COMMENT '换货完成时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_exchange_no (exchange_no),
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no (after_sales_no),
    KEY idx_status_created_at (exchange_status, created_at)
) COMMENT='换货记录表';

-- ============================================================================
-- 16. 补偿记录表
-- ============================================================================
CREATE TABLE compensation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    compensation_no VARCHAR(64) NOT NULL COMMENT '补偿单号',
    after_sales_id BIGINT NOT NULL COMMENT '售后主单ID',
    after_sales_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    compensation_type VARCHAR(32) NOT NULL COMMENT '补偿类型：COUPON/POINTS/BALANCE/MANUAL',
    compensation_amount DECIMAL(18,2) DEFAULT 0 COMMENT '补偿金额或折算金额',
    compensation_status VARCHAR(32) NOT NULL COMMENT '补偿状态',
    external_grant_no VARCHAR(128) COMMENT '外部发放流水号（必须保留）',
    failure_reason VARCHAR(1024) COMMENT '失败原因',
    granted_at DATETIME COMMENT '发放成功时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_compensation_no (compensation_no),
    KEY idx_after_sales_id (after_sales_id),
    KEY idx_after_sales_no (after_sales_no),
    KEY idx_status_created_at (compensation_status, created_at)
) COMMENT='补偿记录表';

-- ============================================================================
-- 17. 幂等记录表
-- ============================================================================
CREATE TABLE idempotency_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等Key（仅来自请求头Idempotency-Key）',
    request_hash VARCHAR(128) NOT NULL COMMENT '请求内容哈希',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id VARCHAR(128) COMMENT '业务ID',
    response_body TEXT COMMENT '成功响应体',
    status VARCHAR(32) NOT NULL COMMENT '状态：PROCESSING/SUCCESS/FAILED',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    KEY idx_biz_type_biz_id (biz_type, biz_id),
    KEY idx_expire_at (expire_at)
) COMMENT='幂等记录表';

-- ============================================================================
-- 18. Agent 调用追踪表
-- ============================================================================
CREATE TABLE agent_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    user_id BIGINT COMMENT '用户ID',
    conversation_id VARCHAR(64) COMMENT '会话ID',
    after_sales_id BIGINT COMMENT '关联售后单ID',
    user_input TEXT COMMENT '用户输入',
    intent VARCHAR(64) COMMENT '识别意图',
    risk_level VARCHAR(32) COMMENT '风险等级',
    final_answer TEXT COMMENT '最终回答',
    status VARCHAR(32) NOT NULL COMMENT '执行状态',
    error_message TEXT COMMENT '错误信息',
    latency_ms BIGINT COMMENT '总耗时',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_conversation_id_created_at (conversation_id, created_at),
    KEY idx_after_sales_id (after_sales_id)
) COMMENT='Agent调用追踪表';

-- ============================================================================
-- 19. Agent 工具调用记录表
-- ============================================================================
CREATE TABLE agent_tool_call (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    tool_input TEXT COMMENT '工具输入',
    tool_output TEXT COMMENT '工具输出',
    success TINYINT NOT NULL COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    latency_ms BIGINT COMMENT '耗时',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_trace_id (trace_id),
    KEY idx_tool_name_created_at (tool_name, created_at)
) COMMENT='Agent工具调用记录表';

-- ============================================================================
-- 20. Agent 高风险动作确认表
-- ============================================================================
CREATE TABLE agent_confirm_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    confirm_token VARCHAR(128) NOT NULL COMMENT '确认令牌',
    trace_id VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    action_type VARCHAR(64) NOT NULL COMMENT '动作类型',
    action_payload TEXT NOT NULL COMMENT '动作参数JSON',
    status VARCHAR(32) NOT NULL COMMENT '状态：WAIT_CONFIRM/CONFIRMED/CANCELLED/EXPIRED',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    confirmed_at DATETIME COMMENT '确认时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_confirm_token (confirm_token),
    KEY idx_trace_id (trace_id),
    KEY idx_user_id_created_at (user_id, created_at),
    KEY idx_status_expire_at (status, expire_at)
) COMMENT='Agent高风险动作确认表';

-- ============================================================================
-- 21. 知识文档表
-- ============================================================================
CREATE TABLE knowledge_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    doc_no VARCHAR(64) NOT NULL COMMENT '知识文档编号',
    doc_type VARCHAR(32) NOT NULL COMMENT '文档类型：POLICY/FAQ/CASE/MANUAL/SCRIPT',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
    source_id VARCHAR(128) COMMENT '来源ID',
    content LONGTEXT NOT NULL COMMENT '文档内容',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_doc_no (doc_no),
    KEY idx_doc_type_status (doc_type, status),
    KEY idx_source_type_source_id (source_type, source_id)
) COMMENT='知识文档表';

-- ============================================================================
-- 22. 知识切片表
-- ============================================================================
CREATE TABLE knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    doc_id BIGINT NOT NULL COMMENT '知识文档ID',
    chunk_no VARCHAR(64) NOT NULL COMMENT '切片编号',
    chunk_index INT NOT NULL COMMENT '切片序号',
    content TEXT NOT NULL COMMENT '切片内容',
    token_count INT COMMENT 'Token数量估算',
    vector_id VARCHAR(128) COMMENT '向量库ID',
    metadata_json TEXT COMMENT '元数据JSON',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_chunk_no (chunk_no),
    KEY idx_doc_id (doc_id),
    KEY idx_vector_id (vector_id)
) COMMENT='知识切片表';

-- ============================================================================
-- 23. 知识构建任务表
-- ============================================================================
CREATE TABLE knowledge_build_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型：AFTER_SALES_CASE/POLICY_DOC/FAQ',
    source_id VARCHAR(128) NOT NULL COMMENT '来源ID',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_source_type_source_id (source_type, source_id),
    KEY idx_status_created_at (task_status, created_at)
) COMMENT='知识构建任务表';

-- ============================================================================
-- 24. RAG 评估集表
-- ============================================================================
CREATE TABLE rag_eval_dataset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    question VARCHAR(1024) NOT NULL COMMENT '评估问题',
    expected_doc_nos TEXT NOT NULL COMMENT '期望命中文档编号，逗号分隔',
    tags VARCHAR(255) COMMENT '标签',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_enabled (enabled)
) COMMENT='RAG评估集表';

-- ============================================================================
-- 25. RAG 评估结果表
-- ============================================================================
CREATE TABLE rag_eval_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    eval_run_no VARCHAR(64) NOT NULL COMMENT '评估批次号',
    question_id BIGINT NOT NULL COMMENT '问题ID',
    question VARCHAR(1024) NOT NULL COMMENT '问题文本',
    retrieved_doc_nos TEXT COMMENT '检索到的文档编号',
    hit_at_3 TINYINT NOT NULL COMMENT 'Recall@3是否命中',
    hit_at_5 TINYINT NOT NULL COMMENT 'Recall@5是否命中',
    first_hit_rank INT COMMENT '首个命中排名',
    reciprocal_rank DECIMAL(10,6) COMMENT '倒数排名',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_eval_run_no (eval_run_no),
    KEY idx_question_id (question_id)
) COMMENT='RAG评估结果表';
