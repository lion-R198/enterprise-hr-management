package com.enterprise.hr.common.security;

import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;

/**
 * 当前登录用户的线程上下文。
 *
 * <p>由 {@link AuthInterceptor} 在 {@code preHandle} 写入、{@code afterCompletion} 清理。
 * 清理动作必须放在 {@code afterCompletion}：容器线程会复用，漏清理会导致
 * "上一个请求的用户"泄漏到下一个请求，产生越权。
 *
 * <p>非 Web 线程（定时任务、单元测试）取不到用户时，{@link #require()} 抛 401，
 * 避免出现"匿名却按管理员处理"的静默越权。
 *
 * @author Enterprise HR Team
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /** 写入当前请求用户（仅拦截器调用） */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 取当前用户，可能为 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前用户，未登录直接抛 401 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /** 当前用户 ID，未登录返回 null（用于日志等弱依赖场景） */
    public static Long currentUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /** 当前用户关联的员工 ID，未登录返回 null */
    public static Long currentEmployeeId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getEmployeeId();
    }

    /** 当前用户姓名，未登录返回 null（日志快照用） */
    public static String currentRealName() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRealName();
    }

    /** 清理，必须在请求结束时调用 */
    public static void clear() {
        HOLDER.remove();
    }
}
