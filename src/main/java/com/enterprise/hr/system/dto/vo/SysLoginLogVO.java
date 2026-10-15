package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 VO。
 *
 * <p>字段与 {@code sys_login_log} 列一一对应，含失败原因（{@code message}）：
 * 审计场景下"谁在什么时候尝试登录但失败了"与成功记录同样重要
 * ——连续失败是撞库的典型信号。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysLoginLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 登录账号 */
    private String username;
    /** 登录 IP */
    private String loginIp;
    /** 登录地点 */
    private String loginLocation;
    /** 浏览器 */
    private String browser;
    /** 操作系统 */
    private String os;
    /** 登录状态：1 成功 / 0 失败 */
    private Integer loginStatus;
    /** 提示信息（失败时为原因：密码错误 / 账号已锁定 等） */
    private String message;
    /** 登录时间 */
    private LocalDateTime loginTime;
}
