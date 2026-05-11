package com.aftersales.api.controller;

import com.aftersales.biz.service.*;
import com.aftersales.common.result.Result;
import com.aftersales.infra.entity.AfterSalesComment;
import com.aftersales.infra.entity.AfterSalesOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 售后接口控制器。只做参数接收和调用业务服务。
 */
@RestController
@RequestMapping("/api/after-sales")
public class AfterSalesController {

    private final AfterSalesApplicationService applicationService;
    private final AfterSalesReviewService reviewService;
    private final RefundService refundService;
    private final ReturnService returnService;
    private final ExchangeService exchangeService;
    private final CompensationService compensationService;
    private final OperationLogService operationLogService;
    private final AfterSalesCommentService commentService;

    public AfterSalesController(AfterSalesApplicationService applicationService,
                                 AfterSalesReviewService reviewService,
                                 RefundService refundService,
                                 ReturnService returnService,
                                 ExchangeService exchangeService,
                                 CompensationService compensationService,
                                 OperationLogService operationLogService,
                                 AfterSalesCommentService commentService) {
        this.applicationService = applicationService;
        this.reviewService = reviewService;
        this.refundService = refundService;
        this.returnService = returnService;
        this.exchangeService = exchangeService;
        this.compensationService = compensationService;
        this.operationLogService = operationLogService;
        this.commentService = commentService;
    }

    // ==================== 售后申请 ====================

    @PostMapping("/applications")
    public Result<Map<String, Object>> create(HttpServletRequest request,
                                               @RequestBody Map<String, Object> body) {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return Result.ok(applicationService.createApplication(idempotencyKey, body));
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) String afterSalesType,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(applicationService.listAfterSales(status, afterSalesType, orderNo, pageNum, pageSize));
    }

    @GetMapping("/{afterSalesNo}")
    public Result<Map<String, Object>> detail(@PathVariable String afterSalesNo) {
        return Result.ok(applicationService.getDetail(afterSalesNo));
    }

    @PostMapping("/{afterSalesNo}/cancel")
    public Result<Void> cancel(@PathVariable String afterSalesNo) {
        applicationService.cancelApplication(afterSalesNo);
        return Result.ok();
    }

    // ==================== 审核 ====================

    @PostMapping("/{afterSalesNo}/review/approve")
    public Result<Map<String, Object>> approve(@PathVariable String afterSalesNo,
                                                @RequestBody Map<String, Object> body) {
        return Result.ok(reviewService.approve(afterSalesNo, body));
    }

    @PostMapping("/{afterSalesNo}/review/reject")
    public Result<Map<String, Object>> reject(@PathVariable String afterSalesNo,
                                               @RequestBody Map<String, Object> body) {
        return Result.ok(reviewService.reject(afterSalesNo, body));
    }

    @PostMapping("/{afterSalesNo}/review/need-more-info")
    public Result<Void> needMoreInfo(@PathVariable String afterSalesNo,
                                      @RequestBody Map<String, Object> body) {
        reviewService.needMoreInfo(afterSalesNo, body);
        return Result.ok();
    }

    // ==================== 退货 ====================

    @PostMapping("/{afterSalesNo}/return/shipment")
    public Result<Void> submitShipment(@PathVariable String afterSalesNo,
                                        @RequestBody Map<String, Object> body) {
        returnService.submitShipment(afterSalesNo, body);
        return Result.ok();
    }

    @PostMapping("/{afterSalesNo}/return/receive")
    public Result<Void> confirmReceive(@PathVariable String afterSalesNo,
                                        @RequestBody Map<String, Object> body) {
        returnService.confirmReceive(afterSalesNo, body);
        return Result.ok();
    }

    @GetMapping("/{afterSalesNo}/return")
    public Result<?> getReturn(@PathVariable String afterSalesNo) {
        return Result.ok(returnService.getReturn(afterSalesNo));
    }

    // ==================== 退款 ====================

    @PostMapping("/{afterSalesNo}/refund/execute")
    public Result<Map<String, Object>> executeRefund(HttpServletRequest request,
                                                      @PathVariable String afterSalesNo,
                                                      @RequestBody Map<String, Object> body) {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return Result.ok(refundService.executeRefund(afterSalesNo, idempotencyKey, body));
    }

    @GetMapping("/{afterSalesNo}/refund")
    public Result<?> getRefund(@PathVariable String afterSalesNo) {
        return Result.ok(refundService.getRefund(afterSalesNo));
    }

    // ==================== 换货 ====================

    @PostMapping("/{afterSalesNo}/exchange/lock-stock")
    public Result<Void> lockStock(@PathVariable String afterSalesNo) {
        exchangeService.lockStock(afterSalesNo);
        return Result.ok();
    }

    @PostMapping("/{afterSalesNo}/exchange/ship")
    public Result<Void> shipExchange(@PathVariable String afterSalesNo,
                                      @RequestBody Map<String, Object> body) {
        exchangeService.ship(afterSalesNo, body);
        return Result.ok();
    }

    @GetMapping("/{afterSalesNo}/exchange")
    public Result<?> getExchange(@PathVariable String afterSalesNo) {
        return Result.ok(exchangeService.getExchange(afterSalesNo));
    }

    // ==================== 补偿 ====================

    @PostMapping("/{afterSalesNo}/compensation/grant")
    public Result<Map<String, Object>> grantCompensation(HttpServletRequest request,
                                                           @PathVariable String afterSalesNo,
                                                           @RequestBody Map<String, Object> body) {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return Result.ok(compensationService.grant(afterSalesNo, idempotencyKey, body));
    }

    @GetMapping("/{afterSalesNo}/compensation")
    public Result<?> getCompensation(@PathVariable String afterSalesNo) {
        return Result.ok(compensationService.getCompensation(afterSalesNo));
    }

    // ==================== 操作日志（只读） ====================

    @GetMapping("/{afterSalesNo}/operation-logs")
    public Result<List<AfterSalesOperationLog>> operationLogs(@PathVariable String afterSalesNo) {
        var asOrder = applicationService.getDetail(afterSalesNo);
        Long id = asOrder.get("order") instanceof com.aftersales.infra.entity.AfterSalesOrder o ? o.getId() : null;
        if (id == null) return Result.ok(List.of());
        return Result.ok(operationLogService.listByAfterSalesId(id));
    }

    // ==================== 评论 ====================

    @PostMapping("/{afterSalesNo}/comments")
    public Result<AfterSalesComment> addComment(@PathVariable String afterSalesNo,
                                                  @RequestBody Map<String, Object> body) {
        return Result.ok(commentService.addComment(afterSalesNo, body));
    }

    @GetMapping("/{afterSalesNo}/comments")
    public Result<List<AfterSalesComment>> listComments(@PathVariable String afterSalesNo) {
        return Result.ok(commentService.listComments(afterSalesNo));
    }
}
