package com.enterprise.hr.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * 统一响应封装。
 *
 * <p>三段式结构：{@code {code, message, data}}，前端依据 {@code code} 判断业务成败。
 * 与前端 {@code src/utils/request.js} 的响应拦截器契约一一对应：
 * <ul>
 *     <li>{@code code === 200} —— resolve({@code data})</li>
 *     <li>{@code code === 401} —— 清除令牌并跳转登录页</li>
 *     <li>其它 code —— 弹出 {@code message} 并 reject</li>
 * </ul>
 *
 * <p>HTTP 状态码与业务 {@code code} 保持同值，便于网关/浏览器网络面板直接识别，
 * 但前端始终以响应体中的 {@code code} 为准。
 *
 * @param <T> 业务数据类型
 * @author Enterprise HR Team
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int CODE_SUCCESS = 200;
    /** 参数或业务校验失败状态码 */
    public static final int CODE_BAD_REQUEST = 400;
    /** 未认证：缺少或非法令牌 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 已认证但无权限 */
    public static final int CODE_FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int CODE_NOT_FOUND = 404;
    /** 资源状态冲突（并发/重复提交） */
    public static final int CODE_CONFLICT = 409;
    /** 服务端异常状态码 */
    public static final int CODE_FAIL = 500;

    /** 状态码：200 成功，400 参数/业务校验失败，401 未认证，403 无权限，404 不存在，500 服务端异常 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public R() {
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功，无数据 */
    public static <T> R<T> ok() {
        return new R<>(CODE_SUCCESS, "success", null);
    }

    /** 成功，携带数据 */
    public static <T> R<T> ok(T data) {
        return new R<>(CODE_SUCCESS, "success", data);
    }

    /** 成功，自定义提示信息 */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(CODE_SUCCESS, message, data);
    }

    /** 失败，默认 500 */
    public static <T> R<T> fail(String message) {
        return new R<>(CODE_FAIL, message, null);
    }

    /** 失败，自定义状态码（如 400 业务校验失败） */
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    /** 失败，按错误码枚举构造 */
    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败，按错误码枚举构造并覆盖提示信息 */
    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    /**
     * 是否为成功响应（供调用方/测试断言）。
     *
     * <p>注意：方法名符合 JavaBean getter 命名约定，会被 Jackson 当作属性序列化出
     * 一个额外的 {@code success} 字段，破坏「响应体恒为三段式」的契约，
     * 因此必须用 {@link JsonIgnore} 显式排除。业务上需要 {@code success} 字段的
     * 结果对象（如 {@code SimpleResultVO}）请自带该字段。
     */
    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == CODE_SUCCESS;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "R{code=" + code + ", message='" + message + "', data=" + data + '}';
    }
}
