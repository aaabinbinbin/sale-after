package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户账户 Mapper。
 */
@Mapper
public interface UserAccountMapper {

    UserAccount selectByUsername(@Param("username") String username);

    UserAccount selectById(@Param("id") Long id);

    int insert(UserAccount userAccount);
}
