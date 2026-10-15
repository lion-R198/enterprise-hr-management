package com.enterprise.hr.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SysLoginLog实体（对应表 {@code sys_login_log}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_login_log")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID(登录失败时可能为空,故不设外键) */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 登录IP */
    private String loginIp;

    /** 登录地点 */
    private String loginLocation;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录状态:1成功 0失败 */
    private Integer loginStatus;

    /** 提示信息 */
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
