package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户列表 VO。
 *
 * <p>与实体 {@code SysUser} 的差异：
 * <ul>
 *     <li><b>不含 {@code password}</b> —— 口令哈希绝不出现在任何响应中</li>
 *     <li>补充展示字段 {@code employeeName} / {@code deptName} / {@code roleNames}</li>
 *     <li>{@code roles} 保留角色编码数组，供前端编辑弹窗回填多选</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private Long employeeId;
    private Long deptId;
    private String phone;
    private String email;
    private String avatar;
    /** 账号状态：1 正常 / 0 停用 / 2 锁定 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    /** 关联员工姓名 */
    private String employeeName;
    /** 所属部门名称 */
    private String deptName;
    /** 角色名称，以「、」拼接（列表展示） */
    private String roleNames;
    /** 角色编码集合（编辑表单回填） */
    private List<String> roles;
}
