package com.enterprise.hr.auth.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息（登录响应与 {@code /auth/me} 共用）。
 *
 * <p><b>刻意使用驼峰字段名</b>：前端 {@code store/modules/user.js} 直接消费
 * {@code userInfo.employeeId} / {@code userInfo.deptId} / {@code userInfo.realName}，
 * 且该结构属于"会话上下文"而非"业务实体"，故不加
 * {@code @JsonNaming(SnakeCaseStrategy)}（业务实体才是蛇形）。
 *
 * <p>不返回手机号、邮箱等非必要信息，也不返回任何口令字段。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;
    /** 登录账号 */
    private String username;
    /** 姓名 */
    private String realName;
    /** 关联员工 ID（非员工账号为 null） */
    private Long employeeId;
    /** 所属部门 ID */
    private Long deptId;
    /** 角色编码集合，前端路由守卫据此过滤菜单 */
    private List<String> roles;
    /** 头像地址 */
    private String avatar;
}
