package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AgentLlmCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Agent LLM 调用记录 Mapper。
 */
@Mapper
public interface AgentLlmCallMapper {

    int insert(AgentLlmCall call);

    List<AgentLlmCall> selectByTraceId(@Param("traceId") String traceId);

    /** 统计指定时间范围内的 token 消耗 */
    List<Map<String, Object>> selectTokenUsageByDate(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);

    /** 按 call_type 汇总 token */
    List<Map<String, Object>> selectTokenUsageByType(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);

    /** 总览统计：总调用次数、成功次数、总 token、平均延迟 */
    Map<String, Object> selectOverviewStats(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate);
}
