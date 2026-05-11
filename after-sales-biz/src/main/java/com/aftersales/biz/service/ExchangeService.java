package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.ExchangeStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.*;
import com.aftersales.infra.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 换货服务。短期库存锁定使用 Redis，MySQL 保存最终库存事实。
 *
 * 流程：审核通过(EXCHANGE_PROCESSING) → 锁定库存 → 用户退回 → 确认收货 → 换货发货 → COMPLETED
 */
@Service
public class ExchangeService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);
    private static final String STOCK_LOCK_PREFIX = "stock:lock:sku:";

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final AfterSalesItemMapper afterSalesItemMapper;
    private final ExchangeRecordMapper exchangeRecordMapper;
    private final SkuStockMapper skuStockMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redisTemplate;

    public ExchangeService(AfterSalesOrderMapper afterSalesOrderMapper,
                            AfterSalesItemMapper afterSalesItemMapper,
                            ExchangeRecordMapper exchangeRecordMapper,
                            SkuStockMapper skuStockMapper,
                            AfterSalesStateMachine stateMachine,
                            OperationLogService operationLogService,
                            StringRedisTemplate redisTemplate) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.afterSalesItemMapper = afterSalesItemMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
        this.skuStockMapper = skuStockMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 锁定换货库存。从售后明细中获取目标 SKU，检查可用库存，Redis 锁定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(String afterSalesNo) {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role))
            throw new BusinessException(ErrorCode.FORBIDDEN);

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        // 从售后明细获取目标 SKU
        List<AfterSalesItem> items = afterSalesItemMapper.selectByAfterSalesId(asOrder.getId());
        if (items.isEmpty()) throw new BusinessException(ErrorCode.AFTER_SALES_STATUS_INVALID, "无售后明细");

        for (AfterSalesItem item : items) {
            // 换货使用 exchange_sku_id（优先）或原 SKU
            Long targetSkuId = item.getExchangeSkuId() != null ? item.getExchangeSkuId() : item.getSkuId();
            int lockQty = item.getApprovedQuantity() > 0 ? item.getApprovedQuantity() : item.getApplyQuantity();

            SkuStock stock = skuStockMapper.selectBySkuId(targetSkuId);
            if (stock == null) throw new BusinessException(ErrorCode.EXCHANGE_STOCK_NOT_ENOUGH, "SKU " + targetSkuId + " 不存在");

            // 读 Redis 当前锁定数
            String lockKey = STOCK_LOCK_PREFIX + targetSkuId;
            String lockedStr = redisTemplate.opsForValue().get(lockKey);
            int locked = lockedStr != null ? Integer.parseInt(lockedStr) : 0;

            int available = stock.getAvailableStock() - locked;
            if (available < lockQty)
                throw new BusinessException(ErrorCode.EXCHANGE_STOCK_NOT_ENOUGH,
                        "SKU " + targetSkuId + " 库存不足(可用:" + available + " 需:" + lockQty + ")");

            // Redis 锁定
            redisTemplate.opsForValue().increment(lockKey, lockQty);
            redisTemplate.expire(lockKey, 30, TimeUnit.MINUTES);
            log.info("换货库存锁定 skuId={} qty={} available={} afterLocked={}", targetSkuId, lockQty, available, locked + lockQty);
        }

        // 创建换货记录
        ExchangeRecord er = exchangeRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (er == null) {
            er = new ExchangeRecord();
            er.setExchangeNo(IdGenerator.genExchangeNo());
            er.setAfterSalesId(asOrder.getId());
            er.setAfterSalesNo(asOrder.getAfterSalesNo());
            er.setExchangeStatus(ExchangeStatus.STOCK_LOCKED.getCode());
            er.setStockLocked(true);
            exchangeRecordMapper.insert(er);
        } else {
            exchangeRecordMapper.updateStockLocked(er.getId(), true);
        }

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "LOCK_EXCHANGE_STOCK", asOrder.getStatus(), asOrder.getStatus(), "锁定换货库存完成");
    }

    /**
     * 换货发货：扣减 MySQL 库存 + 释放 Redis 锁定 + 状态到 COMPLETED。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ship(String afterSalesNo, Map<String, Object> command) {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role))
            throw new BusinessException(ErrorCode.FORBIDDEN);

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        ExchangeRecord er = exchangeRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (er == null || !Boolean.TRUE.equals(er.getStockLocked()))
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_LOCKED);

        // 状态机：当前状态 → COMPLETED
        String oldStatus = asOrder.getStatus();
        stateMachine.checkTransition(AfterSalesStatus.fromCode(oldStatus), AfterSalesStatus.COMPLETED);

        // 更新换货发货信息
        exchangeRecordMapper.updateShipped(er.getId(), ExchangeStatus.SHIPPED.getCode(),
                (String) command.get("logisticsCompany"), (String) command.get("logisticsNo"));

        // 扣减 MySQL 库存 + 释放 Redis 锁定（按明细逐 SKU 处理）
        List<AfterSalesItem> items = afterSalesItemMapper.selectByAfterSalesId(asOrder.getId());
        for (AfterSalesItem item : items) {
            Long targetSkuId = item.getExchangeSkuId() != null ? item.getExchangeSkuId() : item.getSkuId();
            int qty = item.getApprovedQuantity() > 0 ? item.getApprovedQuantity() : item.getApplyQuantity();

            SkuStock stock = skuStockMapper.selectBySkuId(targetSkuId);
            if (stock != null) {
                skuStockMapper.decreaseStock(stock.getId(), qty, stock.getVersion());
                String lockKey = STOCK_LOCK_PREFIX + targetSkuId;
                redisTemplate.opsForValue().decrement(lockKey, qty);
            }
        }

        // 更新售后单到 COMPLETED
        afterSalesOrderMapper.updateComplete(asOrder.getId(),
                AfterSalesStatus.COMPLETED.getCode(), oldStatus, asOrder.getVersion());

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "SHIP_EXCHANGE", oldStatus, AfterSalesStatus.COMPLETED.getCode(), "换货发货完成");
    }

    public ExchangeRecord getExchange(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        return exchangeRecordMapper.selectByAfterSalesId(asOrder.getId());
    }
}
