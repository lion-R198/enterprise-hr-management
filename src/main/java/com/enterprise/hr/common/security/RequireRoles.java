package com.enterprise.hr.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色校验注解（按钮级/接口级权限）。
 *
 * <p>标注在 Controller 类或方法上，由 {@link AuthInterceptor} 统一校验。
 * 方法上的注解优先于类上的注解（方法级可收窄权限）。
 *
 * <p>语义：<b>持有 {@link #value()} 中任意一个角色即放行</b>；
 * {@link #required()} 为 true 时要求全部满足。
 *
 * <pre>{@code
 * @RequireRoles(RoleCode.HR)                       // 仅人事可访问
 * @RequireRoles({RoleCode.HR, RoleCode.ADMIN})     // 人事或管理员
 * }</pre>
 *
 * <p>未标注的接口只要求"已登录"。行级数据范围（能看到哪些行）不在此处理，
 * 由各 Service 通过 {@code DataScopeHelper} 施加。
 *
 * @author Enterprise HR Team
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoles {

    /** 允许访问的角色编码（满足其一即可） */
    String[] value();

    /** 为 true 时要求同时具备全部角色；默认满足其一即可 */
    boolean required() default false;
}
