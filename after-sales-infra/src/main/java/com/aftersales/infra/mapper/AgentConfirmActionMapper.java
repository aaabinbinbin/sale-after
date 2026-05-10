package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AgentConfirmAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentConfirmActionMapper {

    int insert(AgentConfirmAction action);

    AgentConfirmAction selectByToken(@Param("confirmToken") String confirmToken);

    int updateStatus(@Param("confirmToken") String confirmToken, @Param("status") String status);
}
