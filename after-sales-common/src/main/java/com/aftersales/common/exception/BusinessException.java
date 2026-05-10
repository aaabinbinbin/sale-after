package com.aftersales.common.exception;

/**
 * 业务异常。
 *
 * 所有业务校验不通过时抛出此异常，由 GlobalExceptionHandler 统一处理。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.code = errorCode.getCode();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
