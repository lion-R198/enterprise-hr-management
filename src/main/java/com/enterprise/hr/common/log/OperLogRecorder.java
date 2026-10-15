package com.enterprise.hr.common.log;

/**
 * 操作日志落库接口。
 *
 * <p>{@link OperLogAspect}（common 层）通过本接口写日志，具体落库实现放在
 * {@code system} 模块，使 common 层不反向依赖业务模块。同时也便于单测里替换为内存实现。
 *
 * @author Enterprise HR Team
 */
public interface OperLogRecorder {

    /**
     * 记录一条操作日志。
     *
     * <p>实现方必须保证：<b>写日志失败不得影响业务主流程</b>（内部捕获异常并降级为应用日志）。
     *
     * @param record 日志内容
     */
    void record(OperLogRecord record);
}
