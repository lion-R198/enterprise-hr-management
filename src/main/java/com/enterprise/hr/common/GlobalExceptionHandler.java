package com.enterprise.hr.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转换为 {@link R} 三段式响应。
 *
 * <p>设计原则（与前端异常处理对应）：
 * <ol>
 *     <li><b>响应体恒为 {@code {code, message, data}}</b> —— 前端拦截器只认 {@code code}</li>
 *     <li><b>HTTP 状态码与业务 code 同值</b> —— 网关、浏览器网络面板、前端异步错误分支都能正确分流</li>
 *     <li><b>业务规则拦截（BR-xx）不打印堆栈</b> —— 属预期分支，只记 warn，避免淹没真实故障</li>
 *     <li><b>未预期异常不向外暴露堆栈</b>（NF-13：错误业务语义化）</li>
 * </ol>
 *
 * <p>状态码约定：
 * <ul>
 *     <li>400 —— 参数校验失败（{@code @Valid}）、业务校验失败（BR-xx）、请求体不可读</li>
 *     <li>401 —— 未认证：令牌缺失/过期/非法</li>
 *     <li>403 —— 已认证但越权（角色不足、行级数据范围外）</li>
 *     <li>404 —— 请求的资源或接口不存在</li>
 *     <li>405 —— 请求方法不被支持</li>
 *     <li>500 —— 未预期的服务端异常</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------ 业务与认证

    /**
     * 业务异常：明确携带错误码（400 业务校验 / 401 未认证 / 403 越权 / 404 不存在 / 409 冲突）。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        HttpStatus status = HttpStatus.resolve(e.getCode()) == null
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.valueOf(e.getCode());
        // 业务规则拦截是预期分支：只记 warn，不打印堆栈
        log.warn("业务拦截 [{}] {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(status).body(R.fail(e.getCode(), e.getMessage()));
    }

    /** 业务校验失败：Service 层抛出的 IllegalArgumentException（遗留用法，统一按 400 处理） */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(R.CODE_BAD_REQUEST, e.getMessage()));
    }

    /**
     * 业务状态异常（如"批量删除失败"）—— 属于服务端处理未达预期。
     * 归入 500 以免与参数校验混淆。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> handleIllegalState(IllegalStateException e) {
        log.error("业务状态异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(R.CODE_FAIL, e.getMessage()));
    }

    // ------------------------------------------------------------------ 参数校验

    /** 请求体 @Valid 校验失败（@RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return badRequest(joinFieldErrors(e.getBindingResult().getFieldErrors()));
    }

    /** 表单/查询参数绑定校验失败（@ModelAttribute） */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException e) {
        return badRequest(joinFieldErrors(e.getBindingResult().getFieldErrors()));
    }

    /** 方法参数级校验失败（@Validated 标注在类上） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations() == null || e.getConstraintViolations().isEmpty()
                ? "参数校验失败"
                : e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("; "));
        return badRequest(msg.isEmpty() ? "参数校验失败" : msg);
    }

    /** 缺少必填查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少必填参数：" + e.getParameterName());
    }

    /** 参数类型不匹配（如 id 传了非数字） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数类型错误：" + e.getName());
    }

    /** 请求体不可读：JSON 格式错误、类型不匹配、枚举值非法等 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return badRequest("请求体格式错误，请检查 JSON 结构");
    }

    // ------------------------------------------------------------------ 路由与数据库

    /** 请求方法不被支持（如对只读接口发了 POST） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不被支持：" + e.getMethod()));
    }

    /** 接口不存在 */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<R<Void>> handleNoHandler(Exception e) {
        log.warn("接口不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.fail(R.CODE_NOT_FOUND, "请求的接口不存在"));
    }

    /** 唯一键冲突：把数据库约束错误翻译为可读提示，而不是 500 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.fail(R.CODE_CONFLICT, "数据已存在，请检查唯一性字段（如工号 / 编码 / 账号）"));
    }

    /** 外键约束、字段超长等数据完整性问题 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性校验失败: {}", e.getMostSpecificCause().getMessage());
        return badRequest("数据完整性校验失败，请检查关联数据与字段长度");
    }

    // ------------------------------------------------------------------ 兜底

    /** 兜底：未预期的异常，不向外暴露堆栈细节（NF-13） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(R.CODE_FAIL, "系统繁忙，请稍后重试"));
    }

    // ------------------------------------------------------------------ 内部工具

    private ResponseEntity<R<Void>> badRequest(String message) {
        String msg = (message == null || message.isBlank()) ? "参数校验失败" : message;
        log.warn("参数校验失败: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(R.CODE_BAD_REQUEST, msg));
    }

    /**
     * 取全部字段错误消息拼接。
     * 只取首条会让用户"改一个错再报一个"，全部返回可一次性修正。
     */
    private String joinFieldErrors(List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "参数校验失败";
        }
        String msg = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
        return msg.isEmpty() ? "参数校验失败" : msg;
    }
}
