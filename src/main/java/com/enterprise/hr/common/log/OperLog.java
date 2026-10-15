package com.enterprise.hr.common.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统级操作日志注解（F-09-06 日志审计）。
 *
 * <p>标注在 Controller 方法上，由 {@link OperLogAspect} 统一采集并写入 {@code sys_oper_log}。
 * 只标注"改变系统状态"的接口（新增/修改/删除/审批/授权），查询接口不标注
 * ——否则日志表会被列表刷新瞬间淹没，失去审计价值。
 *
 * <pre>{@code
 * @OperLog(module = "员工管理", type = OperType.INSERT, action = "新增员工")
 * }</pre>
 *
 * @author Enterprise HR Team
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 所属模块，如「员工管理」 */
    String module();

    /** 操作描述，如「新增员工」 */
    String action();

    /** 操作类型 */
    OperType type() default OperType.OTHER;

    /**
     * 是否记录请求参数。
     * 涉及手机号、身份证、薪资等敏感信息时保持默认 false，
     * 只记录业务主键与操作人，避免日志表本身成为数据泄露面。
     */
    boolean saveParams() default false;
}
