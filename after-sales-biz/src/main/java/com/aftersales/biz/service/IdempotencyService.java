package com.aftersales.biz.service;

import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.entity.IdempotencyRecord;
import com.aftersales.infra.mapper.IdempotencyRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 幂等服务。
 *
 * 幂等 Key 仅从请求头 Idempotency-Key 读取。
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    public IdempotencyService(IdempotencyRecordMapper idempotencyRecordMapper) {
        this.idempotencyRecordMapper = idempotencyRecordMapper;
    }

    /**
     * 幂等前置校验 / 记录。
     *
     * @param idempotencyKey 幂等 Key（来自请求头）
     * @param requestBody    请求体（用于计算 hash）
     * @param bizType        业务类型
     * @return 如果已成功返回历史响应 JSON；如果处理中抛异常；如果新请求返回 null 表示继续执行
     */
    public String checkOrRecord(String idempotencyKey, Object requestBody, String bizType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENT_MISSING_KEY);
        }

        String requestHash = DigestUtils.md5DigestAsHex(
                JsonUtils.toJson(requestBody).getBytes(StandardCharsets.UTF_8));

        IdempotencyRecord existing = idempotencyRecordMapper.selectByKey(idempotencyKey);

        if (existing != null) {
            if ("SUCCESS".equals(existing.getStatus())) {
                // 相同 key 且 hash 相同 -> 返回历史结果
                if (requestHash.equals(existing.getRequestHash())) {
                    log.info("幂等命中 key={} bizType={}", idempotencyKey, bizType);
                    return existing.getResponseBody();
                }
                // hash 不同 -> 拒绝
                throw new BusinessException(ErrorCode.IDEMPOTENT_KEY_MISMATCH);
            }
            if ("PROCESSING".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.IDEMPOTENT_PROCESSING);
            }
        }

        // 新请求：插入 PROCESSING 记录
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setBizType(bizType);
        record.setStatus("PROCESSING");
        record.setExpireAt(LocalDateTime.now().plusDays(7));
        idempotencyRecordMapper.insert(record);

        return null; // 表示需要继续执行业务
    }

    /**
     * 标记幂等成功。
     */
    public void markSuccess(String idempotencyKey, String responseBody, String bizId) {
        IdempotencyRecord record = idempotencyRecordMapper.selectByKey(idempotencyKey);
        if (record != null) {
            idempotencyRecordMapper.updateSuccess(record.getId(), responseBody, bizId, "SUCCESS");
        }
    }
}
