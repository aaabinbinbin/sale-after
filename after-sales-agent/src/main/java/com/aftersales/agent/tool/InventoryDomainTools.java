package com.aftersales.agent.tool;

import com.aftersales.infra.entity.SkuStock;
import com.aftersales.infra.mapper.SkuStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 库存领域工具聚合。
 */
@Component
public class InventoryDomainTools {

    private static final Logger log = LoggerFactory.getLogger(InventoryDomainTools.class);

    private final SkuStockMapper skuStockMapper;

    public InventoryDomainTools(SkuStockMapper skuStockMapper) {
        this.skuStockMapper = skuStockMapper;
    }

    /**
     * 检查库存可换货性。
     *
     * @param skuId 目标 SKU ID
     * @return 包含可用库存、是否可换货的聚合结果
     */
    public Map<String, Object> checkExchangeAvailability(Long skuId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            SkuStock stock = skuStockMapper.selectBySkuId(skuId);
            if (stock == null) {
                result.put("exists", false);
                result.put("canExchange", false);
                result.put("reason", "SKU 不存在");
                return result;
            }
            result.put("skuId", skuId);
            result.put("exists", true);
            result.put("availableStock", stock.getAvailableStock());
            result.put("totalStock", stock.getTotalStock());
            result.put("canExchange", stock.getAvailableStock() > 0);
            log.info("InventoryDomainTools: 库存检查完成 skuId={} available={}", skuId, stock.getAvailableStock());
        } catch (Exception e) {
            log.error("库存检查失败 skuId={}: {}", skuId, e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }
}
