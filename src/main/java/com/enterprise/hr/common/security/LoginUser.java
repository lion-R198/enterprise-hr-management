package com.enterprise.hr.common.security;

import com.enterprise.hr.common.constant.DataScopeType;
import com.enterprise.hr.common.constant.RoleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文对象（请求级）。
 *
 * <p>每次请求由 {@link AuthInterceptor} 依据令牌中的用户 ID <b>实时查库</b>重建，
 * 因此停用账号、调整角色、变更部门都会在下一次请求立即生效，不存在"令牌里角色过期"的越权窗口。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（sys_user.id） */
    private Long userId;
    /** 登录账号 */
    private String username;
    /** 姓名 */
    private String realName;
    /** 关联员工 ID，非员工账号（如纯运维账号）为 null */
    private Long employeeId;
    /** 所属部门 ID，用于行级数据范围过滤 */
    private Long deptId;
    /** 头像 */
    private String avatar;
    /** 角色编码集合，如 [ROLE_HR, ROLE_MANAGER] */
    private Set<String> roles;
    /** 生效数据范围（多角色取最宽） */
    private DataScopeType dataScope;
    /** 自定义数据范围涉及的部门 ID 列表（data_scope = 5 时使用） */
    private List<Long> scopeDeptIds;

    /** 是否持有指定角色之一 */
    public boolean hasAnyRole(String... codes) {
        if (roles == null || roles.isEmpty() || codes == null || codes.length == 0) {
            return false;
        }
        return Arrays.stream(codes).anyMatch(roles::contains);
    }

    /** 是否持有全部指定角色 */
    public boolean hasAllRoles(String... codes) {
        if (roles == null || roles.isEmpty() || codes == null || codes.length == 0) {
            return false;
        }
        return Arrays.stream(codes).allMatch(roles::contains);
    }

    public boolean isAdmin() {
        return hasAnyRole(RoleCode.ADMIN);
    }

    public boolean isHr() {
        return hasAnyRole(RoleCode.HR);
    }

    public boolean isManager() {
        return hasAnyRole(RoleCode.MANAGER);
    }

    public boolean isFinance() {
        return hasAnyRole(RoleCode.FINANCE);
    }

    public boolean isEmployeeOnly() {
        return hasAnyRole(RoleCode.EMPLOYEE)
                && !hasAnyRole(RoleCode.ADMIN, RoleCode.HR, RoleCode.MANAGER, RoleCode.FINANCE, RoleCode.EXECUTIVE);
    }

    /** 是否不受行级数据范围限制（管理员 / 人事 / 财务 / 决策层） */
    public boolean isUnrestricted() {
        return hasAnyRole(RoleCode.ADMIN, RoleCode.HR, RoleCode.FINANCE, RoleCode.EXECUTIVE)
                || (dataScope != null && dataScope.isAll());
    }

    public Set<String> getRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }

    public DataScopeType getDataScope() {
        return dataScope == null ? DataScopeType.SELF : dataScope;
    }

    /** 便捷构造：可变角色集合 */
    public static Set<String> roleSet(String... codes) {
        return codes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(Arrays.asList(codes));
    }
}
