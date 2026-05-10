package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.ExchangeStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.ExchangeRecord;
import com.aftersales.infra.entity.SkuStock;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import com.aftersales.infra.mapper.ExchangeRecordMapper;
import com.aftersales.infra.mapper.SkuStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 换货服务。
 *
 * 短期库存锁定使用 Redis，MySQL 保存最终库存事实。
 */
@Service
public class ExchangeService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);
    private static final String STOCK_LOCK_PREFIX = "stock:lock:sku:";

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final ExchangeRecordMapper exchangeRecordMapper;
    private final SkuStockMapper skuStockMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redisTemplate;

    public ExchangeService(AfterSalesOrderMapper afterSalesOrderMapper,
                            ExchangeRecordMapper exchangeRecordMapper,
                            SkuStockMapper skuStockMapper,
                            AfterSalesStateMachine stateMachine,
                            OperationLogService operationLogService,
                            StringRedisTemplate redisTemplate) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.exchangeRecordMapper = exchangeRecordMapper;
        this.skuStockMapper = skuStockMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.redisTemplate = redisTemplate;
    }

    /** 锁定换货库存 */
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(String afterSalesNo, Long skuId, int lockQuantity) {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role)) throw new BusinessException(ErrorCode.FORBIDDEN);

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        // 读取 MySQL 可售库存
        SkuStock stock = skuStockMapper.selectBySkuId(skuId);
        if (stock == null) throw new BusinessException(ErrorCode.EXCHANGE_STOCK_NOT_ENOUGH, "SKU不存在");

        // 读取 Redis 当前锁定数量
        String lockKey = STOCK_LOCK_PREFIX + skuId;
        String lockedStr = redisTemplate.opsForValue().get(lockKey);
        int locked = lockedStr != null ? Integer.parseInt(lockedStr) : 0;

        // 判断可用库存
        int available = stock.getAvailableStock() - locked;
        if (available < lockQuantity) {
            throw new BusinessException(ErrorCode.EXCHANGE_STOCK_NOT_ENOUGH,
                    "SKU " + skuId + " 可用库存不足(可用:" + available + " 需求:" + lockQuantity + ")");
        }

        // Redis 增加锁定数量，设置 TTL
        redisTemplate.opsForValue().increment(lockKey, lockQuantity);
        redisTemplate.expire(lockKey, 30, TimeUnit.MINUTES);

        // 创建/更新换货记录
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

        log.info("换货库存锁定成功 skuId={} quantity={} available={} locked={}", skuId, lockQuantity, available, locked + lockQuantity);

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "LOCK_STOCK", asOrder.getStatus(), asOrder.getStatus(),
                "锁定换货库存 SKU:" + skuId + " 数量:" + lockQuantity);
    }

    /** 换货发货（扣减 MySQL 库存 + 释放 Redis 锁定） */
    @Transactional(rollbackFor = Exception.class)
    public void ship(String afterSalesNo, Map<String, Object> command) {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role)) throw new BusinessException(ErrorCode.FORBIDDEN);

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        ExchangeRecord er = exchangeRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (er == null || !Boolean.TRUE.equals(er.getStockLocked())) {
            throw new BusinessException(ErrorCode.EXCHANGE_NOT_LOCKED);
        }

        // 状态流转
        stateMachine.checkTransition(AfterSalesStatus.fromCode(asOrder.getStatus()), AfterSalesStatus.COMPLETED);

        // 更新换货发货信息
        exchangeRecordMapper.updateShipped(er.getId(), ExchangeStatus.SHIPPED.getCode(),
                (String) command.get("logisticsCompany"), (String) command.get("logisticsNo"));

        // 扣减 MySQL 库存，释放 Redis 锁定
        SkuStock stock = skuStockMapper.selectBySkuId(1L); // TODO: 从售后明细获取实际SKU
        if (stock != null) {
            skuStockMapper.decreaseStock(stock.getId(), 1, stock.getVersion());
            // 释放 Redis 锁定
            String lockKey = STOCK_LOCK_PREFIX + stock.getSkuId();
            redisTemplate.opsForValue().decrement(lockKey, 1);
        }

        afterSalesOrderMapper.updateComplete(asOrder.getId(),
                AfterSalesStatus.COMPLETED.getCode(), asOrder.getStatus(), asOrder.getVersion());

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "SHIP_EXCHANGE", asOrder.getStatus(), AfterSalesStatus.COMPLETED.getCode(),
                "换货发货");
    }

    public ExchangeRecord getExchange(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        return exchangeRecordMapper.selectByAfterSalesId(asOrder.getId());
    }
}
