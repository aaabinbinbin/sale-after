package com.aftersales.domain.statemachine;

import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 售后状态机单元测试。
 */
class AfterSalesStateMachineTest {

    private AfterSalesStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new AfterSalesStateMachine();
    }

    @Test
    void shouldAllowCreatedToPendingReview() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.CREATED, AfterSalesStatus.PENDING_REVIEW));
    }

    @Test
    void shouldAllowPendingReviewToApproved() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.PENDING_REVIEW, AfterSalesStatus.APPROVED));
    }

    @Test
    void shouldAllowPendingReviewToRejected() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.PENDING_REVIEW, AfterSalesStatus.REJECTED));
    }

    @Test
    void shouldAllowNeedMoreInfoToPendingReview() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.NEED_MORE_INFO, AfterSalesStatus.PENDING_REVIEW));
    }

    @Test
    void shouldAllowWaitReturnShipmentToWaitReturnReceive() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.WAIT_RETURN_SHIPMENT, AfterSalesStatus.WAIT_RETURN_RECEIVE));
    }

    @Test
    void shouldAllowRefundProcessingToCompleted() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.REFUND_PROCESSING, AfterSalesStatus.COMPLETED));
    }

    @Test
    void shouldThowOnIllegalTransition() {
        // 终态不允许再变更
        assertThrows(BusinessException.class, () ->
                stateMachine.checkTransition(AfterSalesStatus.COMPLETED, AfterSalesStatus.PENDING_REVIEW));
        assertThrows(BusinessException.class, () ->
                stateMachine.checkTransition(AfterSalesStatus.REJECTED, AfterSalesStatus.PENDING_REVIEW));
    }

    @Test
    void shouldThrowOnSkipState() {
        // CREATED 不能直接跳到 COMPLETED
        assertThrows(BusinessException.class, () ->
                stateMachine.checkTransition(AfterSalesStatus.CREATED, AfterSalesStatus.COMPLETED));
    }

    @Test
    void sameStateShouldBeOk() {
        assertDoesNotThrow(() ->
                stateMachine.checkTransition(AfterSalesStatus.PENDING_REVIEW, AfterSalesStatus.PENDING_REVIEW));
    }
}
