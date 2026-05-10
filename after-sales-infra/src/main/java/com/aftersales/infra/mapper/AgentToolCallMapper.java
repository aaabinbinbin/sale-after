package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AgentToolCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentToolCallMapper {

    int insert(AgentToolCall call);

    List<AgentToolCall> selectByTraceId(@Param("traceId") String traceId);
}
