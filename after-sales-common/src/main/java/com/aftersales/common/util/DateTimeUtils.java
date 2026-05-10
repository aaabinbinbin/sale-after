package com.aftersales.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 */
public class DateTimeUtils {

    public static final DateTimeFormatter STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtils() {}

    /** 格式化当前时间 */
    public static String nowStr() {
        return LocalDateTime.now().format(STANDARD);
    }

    /** 格式化指定时间 */
    public static String format(LocalDateTime time) {
        return time != null ? time.format(STANDARD) : null;
    }
}
