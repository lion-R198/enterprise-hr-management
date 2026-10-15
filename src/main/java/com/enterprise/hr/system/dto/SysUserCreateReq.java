package com.enterprise.hr.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新增系统用户请求。
 *
 * @author Enterprise HR Team
 */
@Data
public class SysUserCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录账号（唯一） */
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64, message = "登录账号长度不得超过 64")
    private String username;

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不得超过 32")
    private String realName;

    /**
     * 初始密码。
     * 长度校验在 Service 层完成（BR-09-01 要求不少于 8 位），
     * 这样同一套强度规则也能复用于"重置密码"场景。
     */
    private String password;

    /** 关联员工 ID */
    private Long employeeId;

    /** 所属部门 ID */
    private Long deptId;

    /** 角色编码集合，缺省为 ROLE_EMPLOYEE */
    private List<String> roles;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
