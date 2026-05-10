package com.aftersales.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一接口返回体。
 *
 * 所有 Controller 接口统一使用此类包装返回结果，
 * 前端和 Agent 均可通过 success/code/message/data 判断请求结果。
 *
 * @param <T> data 泛型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 请求是否成功 */
    private boolean success;

    /** 业务状态码 */
    private String code;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    private Result() {}

    // ========== 工厂方法 ==========

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.code = "SUCCESS";
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> Result<T> fail(String code, String message, T data) {
        Result<T> result = fail(code, message);
        result.data = data;
        return result;
    }

    // ========== getter / setter ==========

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
