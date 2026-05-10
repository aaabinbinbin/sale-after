package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AgentTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentTraceMapper {

    int insert(AgentTrace trace);

    AgentTrace selectByTraceId(@Param("traceId") String traceId);

    List<AgentTrace> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    int updateResult(@Param("traceId") String traceId,
                     @Param("intent") String intent,
                     @Param("riskLevel") String riskLevel,
                     @Param("finalAnswer") String finalAnswer,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("latencyMs") Long latencyMs);
}
