package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AfterSalesComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AfterSalesCommentMapper {

    int insert(AfterSalesComment comment);

    List<AfterSalesComment> selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    List<AfterSalesComment> selectByAfterSalesNo(@Param("afterSalesNo") String afterSalesNo);
}
