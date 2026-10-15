package com.enterprise.hr.common.log;

import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 系统级操作日志切面：拦截 {@link OperLog} 标注的方法，采集并异步落库。
 *
 * <p>采集内容：操作人（ID + 姓名快照）、模块、动作、请求方法/URL、客户端 IP、
 * 执行结果（成功/失败及原因）、耗时。
 *
 * <p>可靠性约定：<b>写日志永远不能影响业务</b>。
 * <ul>
 *     <li>业务异常照常抛出（先记录失败日志，再 rethrow）</li>
 *     <li>日志落库自身异常被吞掉并降级为应用日志，避免"日志写失败导致业务失败"</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    /** 成功 */
    private static final int STATUS_SUCCESS = 1;
    /** 失败 */
    private static final int STATUS_FAIL = 0;
    /** 请求参数落库最大长度，防止超长文本把日志表写爆 */
    private static final int MAX_PARAM_LENGTH = 2000;

    private final OperLogRecorder recorder;

    public OperLogAspect(OperLogRecorder recorder) {
        this.recorder = recorder;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            // 无论成败都要留痕：审计价值最高的恰恰是"失败/被拦截"的那些操作
            try {
                save(joinPoint, operLog, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("操作日志落库失败（不影响业务）: module={} action={}", operLog.module(), operLog.action(), e);
            }
        }
    }

    private void save(ProceedingJoinPoint joinPoint, OperLog operLog, Throwable error, long cost) {
        LoginUser user = UserContext.get();
        HttpServletRequest request = IpUtil.currentRequest();

        OperLogRecord.OperLogRecordBuilder builder = OperLogRecord.builder()
                .module(operLog.module())
                .operDesc(operLog.action())
                .operType(operLog.type())
                .operUserId(user == null ? null : user.getUserId())
                .operUserName(user == null ? null : user.getRealName())
                .operIp(IpUtil.getClientIp(request))
                .operStatus(error == null ? STATUS_SUCCESS : STATUS_FAIL)
                .resultMessage(error == null ? "成功" : truncate(error.getMessage(), 500))
                .costTime(cost);

        if (request != null) {
            builder.requestMethod(request.getMethod())
                    .requestUrl(truncate(request.getRequestURI(), 500));
        }
        if (operLog.saveParams()) {
            builder.requestParam(truncate(Arrays.toString(joinPoint.getArgs()), MAX_PARAM_LENGTH));
        }

        recorder.record(builder.build());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
