package com.enterprise.hr.system.log;

import com.enterprise.hr.common.log.OperLogRecord;
import com.enterprise.hr.common.log.OperLogRecorder;
import com.enterprise.hr.system.entity.SysOperLog;
import com.enterprise.hr.system.mapper.SysOperLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志落库实现：把 {@link OperLogRecord} 写入 {@code sys_oper_log}。
 *
 * <p>放在 {@code system} 模块而非 {@code common}，是为了让 common 层不反向依赖业务模块
 * ——切面依赖 {@link OperLogRecorder} 接口，具体实现由 Spring 注入。
 *
 * <p><b>可靠性约定</b>：本方法与业务主流程共用线程，写日志失败时只降级为应用日志，
 * 绝不向上抛出——"审计写失败导致业务回滚"是本末倒置。
 * 同时使用独立事务（{@code REQUIRES_NEW} 级别的语义由调用方保证：切面在业务事务提交后
 * 才调用本方法，因此这里不做事务标注，避免嵌套事务把业务异常状态带进来）。
 *
 * @author Enterprise HR Team
 */
@Component
public class DbOperLogRecorder implements OperLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(DbOperLogRecorder.class);

    /** 成功 */
    private static final int STATUS_SUCCESS = 1;

    /** 异常信息最大长度，与 {@code sys_oper_log.error_msg} 的 varchar(1000) 对齐 */
    private static final int MAX_ERROR_LENGTH = 1000;
    /** 请求参数最大长度，与 {@code sys_oper_log.request_param} 的实际承载能力对齐 */
    private static final int MAX_PARAM_LENGTH = 2000;

    private final SysOperLogMapper operLogMapper;

    public DbOperLogRecorder(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Override
    public void record(OperLogRecord record) {
        if (record == null) {
            return;
        }
        try {
            SysOperLog entity = new SysOperLog();
            entity.setUserId(record.getOperUserId());
            entity.setUsername(record.getOperUserName());
            entity.setModule(record.getModule());
            entity.setOperType(record.getOperType() == null ? null : record.getOperType().getDbCode());
            entity.setOperDesc(record.getOperDesc());
            entity.setRequestMethod(record.getRequestMethod());
            entity.setRequestUrl(record.getRequestUrl());
            entity.setOperIp(record.getOperIp());
            entity.setOperStatus(record.getOperStatus());
            entity.setCostTime(record.getCostTime());
            entity.setOperTime(LocalDateTime.now());

            // 失败日志才是审计价值最高的部分：把原因落到 error_msg，
            // 成功的记录无需重复存一份"成功"文本
            if (record.getOperStatus() != null && record.getOperStatus() != STATUS_SUCCESS) {
                entity.setErrorMsg(truncate(record.getResultMessage(), MAX_ERROR_LENGTH));
            }
            entity.setRequestParam(truncate(record.getRequestParam(), MAX_PARAM_LENGTH));

            operLogMapper.insert(entity);
        } catch (Exception e) {
            // 降级：审计失败不能影响业务
            log.error("操作日志落库失败：module={} action={} url={}",
                    record.getModule(), record.getOperDesc(), record.getRequestUrl(), e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
