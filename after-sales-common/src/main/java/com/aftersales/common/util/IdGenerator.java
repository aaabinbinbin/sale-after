package com.aftersales.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成器。
 *
 * 生成格式：前缀 + yyyyMMddHHmmss + 4位随机数
 * 例如：AS20260510143025A1B2
 */
public class IdGenerator {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private IdGenerator() {}

    /** 生成售后单号 */
    public static String genAfterSalesNo() {
        return gen("AS");
    }

    /** 生成退款单号 */
    public static String genRefundNo() {
        return gen("RF");
    }

    /** 生成退货单号 */
    public static String genReturnNo() {
        return gen("RT");
    }

    /** 生成换货单号 */
    public static String genExchangeNo() {
        return gen("EX");
    }

    /** 生成补偿单号 */
    public static String genCompensationNo() {
        return gen("CP");
    }

    /** 生成知识文档编号 */
    public static String genKnowledgeDocNo() {
        return gen("KD");
    }

    /** 生成知识构建任务编号 */
    public static String genBuildTaskNo() {
        return gen("KT");
    }

    /** 生成评估批次号 */
    public static String genEvalRunNo() {
        return gen("EV");
    }

    /** 生成 Trace ID */
    public static String genTraceId() {
        return gen("TR");
    }

    /** 生成 Confirm Token */
    public static String genConfirmToken() {
        return "confirm-" + gen("");
    }

    private static String gen(String prefix) {
        String time = LocalDateTime.now().format(DTF);
        String rand = randomChars(4);
        return prefix + time + rand;
    }

    private static String randomChars(int len) {
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
