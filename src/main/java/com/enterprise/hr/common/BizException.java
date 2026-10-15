package com.enterprise.hr.common;

import lombok.Getter;

/**
 * 业务异常：Service 层业务规则不满足时抛出，由 {@link GlobalExceptionHandler} 统一翻译为 {@link R}。
 *
 * <p>与 {@link IllegalArgumentException} 的分工：
 * <ul>
 *     <li>{@code IllegalArgumentException} —— 遗留用法，按 400 处理，保留以兼容既有 Service 实现</li>
 *     <li>{@code BizException} —— 明确携带 {@link ErrorCode}（含 404 / 403 / 409 等非 400 语义）</li>
 * </ul>
 *
 * <p>本异常不打印堆栈：业务规则拦截属于"预期内"的流程分支，逐条堆栈会淹没真正的故障日志。
 *
 * @author Enterprise HR Team
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码（同时作为 HTTP 状态码） */
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /** 默认按 400 业务校验失败处理 */
    public BizException(String message) {
        super(message);
        this.code = ErrorCode.BIZ_ERROR.getCode();
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    // ---------------- 静态工厂：语义化抛错，减少调用方样板 ----------------

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }

    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }

    /** 资源不存在（404） */
    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    /** 越权访问（403） */
    public static BizException forbidden(String message) {
        return new BizException(ErrorCode.FORBIDDEN, message);
    }

    /** 数据范围越权（403），统一提示语 */
    public static BizException dataScopeDenied() {
        return new BizException(ErrorCode.DATA_SCOPE_DENIED);
    }

    /** 参数校验失败（400） */
    public static BizException badRequest(String message) {
        return new BizException(ErrorCode.PARAM_INVALID, message);
    }
}
