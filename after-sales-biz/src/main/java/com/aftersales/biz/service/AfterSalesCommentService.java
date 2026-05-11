package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.AfterSalesComment;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.mapper.AfterSalesCommentMapper;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 售后评论服务。
 * 用户可见评论与客服内部备注通过 internalOnly 区分。
 */
@Service
public class AfterSalesCommentService {

    private final AfterSalesCommentMapper commentMapper;
    private final AfterSalesOrderMapper afterSalesOrderMapper;

    public AfterSalesCommentService(AfterSalesCommentMapper commentMapper,
                                     AfterSalesOrderMapper afterSalesOrderMapper) {
        this.commentMapper = commentMapper;
        this.afterSalesOrderMapper = afterSalesOrderMapper;
    }

    /** 添加评论 */
    public AfterSalesComment addComment(String afterSalesNo, Map<String, Object> command) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        AfterSalesComment comment = new AfterSalesComment();
        comment.setAfterSalesId(asOrder.getId());
        comment.setAfterSalesNo(asOrder.getAfterSalesNo());
        comment.setCommenterId(UserContext.getUserId());
        comment.setCommenterRole(UserContext.getRole());
        comment.setContent((String) command.get("content"));
        // 客服可选择标记为内部备注
        Object internal = command.get("internalOnly");
        comment.setInternalOnly(internal instanceof Boolean b ? b : false);
        commentMapper.insert(comment);
        return comment;
    }

    /** 查询评论列表。用户看不到 internal_only=1 的评论 */
    public List<AfterSalesComment> listComments(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        String role = UserContext.getRole();
        List<AfterSalesComment> all = commentMapper.selectByAfterSalesId(asOrder.getId());

        // 普通用户过滤掉内部备注
        if ("USER".equals(role)) {
            return all.stream().filter(c -> !Boolean.TRUE.equals(c.getInternalOnly())).toList();
        }
        return all;
    }
}
