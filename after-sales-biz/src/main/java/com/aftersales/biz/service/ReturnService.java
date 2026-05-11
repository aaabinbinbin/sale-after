package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.AfterSalesType;
import com.aftersales.common.enums.ReturnStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.ReturnRecord;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import com.aftersales.infra.mapper.ReturnRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 退货服务。处理退货物流提交和客服收货确认。
 */
@Service
public class ReturnService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final ReturnRecordMapper returnRecordMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;

    public ReturnService(AfterSalesOrderMapper afterSalesOrderMapper,
                          ReturnRecordMapper returnRecordMapper,
                          AfterSalesStateMachine stateMachine,
                          OperationLogService operationLogService) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.returnRecordMapper = returnRecordMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
    }

    /** 用户填写退货物流：WAIT_RETURN_SHIPMENT → WAIT_RETURN_RECEIVE */
    @Transactional(rollbackFor = Exception.class)
    public void submitShipment(String afterSalesNo, Map<String, Object> command) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        if (!asOrder.getUserId().equals(UserContext.getUserId())) throw new BusinessException(ErrorCode.FORBIDDEN);

        String oldStatus = asOrder.getStatus();
        stateMachine.checkTransition(AfterSalesStatus.fromCode(oldStatus), AfterSalesStatus.WAIT_RETURN_RECEIVE);

        // 创建/更新退货记录
        ReturnRecord rr = returnRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (rr == null) {
            rr = new ReturnRecord();
            rr.setReturnNo(IdGenerator.genReturnNo());
            rr.setAfterSalesId(asOrder.getId());
            rr.setAfterSalesNo(asOrder.getAfterSalesNo());
            rr.setReturnStatus(ReturnStatus.WAIT_SHIPMENT.getCode());
            returnRecordMapper.insert(rr);
        }
        returnRecordMapper.updateLogistics(rr.getId(),
                (String) command.get("logisticsCompany"),
                (String) command.get("logisticsNo"),
                ReturnStatus.SHIPPED.getCode());

        afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                AfterSalesStatus.WAIT_RETURN_RECEIVE.getCode(), oldStatus, asOrder.getVersion());

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "SUBMIT_RETURN_SHIPMENT", oldStatus, AfterSalesStatus.WAIT_RETURN_RECEIVE.getCode(),
                "用户已寄回，物流:" + command.get("logisticsNo"));
    }

    /** 客服确认收货：WAIT_RETURN_RECEIVE → 按售后类型决定下一状态 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String afterSalesNo, Map<String, Object> command) {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role))
            throw new BusinessException(ErrorCode.FORBIDDEN);

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        String oldStatus = asOrder.getStatus();

        // 根据售后类型决定下一个状态
        AfterSalesType type = AfterSalesType.fromCode(asOrder.getAfterSalesType());
        AfterSalesStatus nextStatus;
        if (type == AfterSalesType.EXCHANGE) {
            nextStatus = AfterSalesStatus.EXCHANGE_PROCESSING;
        } else {
            nextStatus = AfterSalesStatus.REFUND_PROCESSING;
        }

        stateMachine.checkTransition(AfterSalesStatus.fromCode(oldStatus), nextStatus);

        // 更新退货记录
        ReturnRecord rr = returnRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (rr != null) {
            returnRecordMapper.updateReceived(rr.getId(), ReturnStatus.RECEIVED.getCode(),
                    UserContext.getUserId(), (String) command.get("receiverRemark"));
        }

        afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                nextStatus.getCode(), oldStatus, asOrder.getVersion());

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "CONFIRM_RETURN_RECEIVE", oldStatus, nextStatus.getCode(), "客服确认收货");
    }

    public ReturnRecord getReturn(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        return returnRecordMapper.selectByAfterSalesId(asOrder.getId());
    }
}
