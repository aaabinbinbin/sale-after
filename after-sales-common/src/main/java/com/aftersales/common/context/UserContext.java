package com.aftersales.common.context;

/**
 * 当前请求用户上下文。
 *
 * 通过 ThreadLocal 保存当前请求用户信息，
 * 由 AuthInterceptor 在请求进入时设置，请求结束时清理。
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    /** 设置当前用户信息 */
    public static void set(UserInfo userInfo) {
        HOLDER.set(userInfo);
    }

    /** 获取当前用户信息 */
    public static UserInfo get() {
        return HOLDER.get();
    }

    /** 获取当前用户 ID */
    public static Long getUserId() {
        UserInfo info = get();
        return info != null ? info.getUserId() : null;
    }

    /** 获取当前用户角色 */
    public static String getRole() {
        UserInfo info = get();
        return info != null ? info.getRole() : null;
    }

    /** 清除当前用户信息 */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 用户信息快照。
     */
    public static class UserInfo {
        private final Long userId;
        private final String username;
        private final String role;

        public UserInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}
