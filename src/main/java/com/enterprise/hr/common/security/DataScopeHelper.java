package com.enterprise.hr.common.security;

import com.enterprise.hr.common.constant.DataScopeType;
import com.enterprise.hr.common.constant.RoleCode;

import java.util.List;

/**
 * 行级数据范围判定工具（NF-10）。
 *
 * <p>对应需求中的三种可见范围：
 * <ul>
 *     <li><b>全量</b> —— 管理员 / 人事 / 财务 / 决策层（角色直接决定，不看 data_scope）</li>
 *     <li><b>本部门</b> —— 部门主管（{@code data_scope = 3}：本部门及下级）</li>
 *     <li><b>仅本人</b> —— 普通员工（{@code data_scope = 4}）</li>
 * </ul>
 *
 * <p>只做"判定"，不做"查询"。具体模块在 Mapper 条件里使用这些判定结果拼装 WHERE，
 * 这样可以保持 SQL 可控、可解释，避免全局自动拼 SQL 带来的隐式越权。
 *
 * <pre>{@code
 * LoginUser user = UserContext.require();
 * LambdaQueryWrapper<Employee> w = Wrappers.lambdaQuery();
 * if (!DataScopeHelper.isUnlimited(user)) {
 *     w.eq(Employee::getDeptId, DataScopeHelper.deptId(user));
 * }
 * }</pre>
 *
 * @author Enterprise HR Team
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    /** 是否可查看全量数据（不受行级限制） */
    public static boolean isUnlimited(LoginUser user) {
        if (user == null) {
            return false;
        }
        if (user.hasAnyRole(RoleCode.ADMIN, RoleCode.HR, RoleCode.FINANCE, RoleCode.EXECUTIVE)) {
            return true;
        }
        return user.getDataScope().isAll();
    }

    /** 是否仅能查看本人数据 */
    public static boolean isSelfOnly(LoginUser user) {
        if (user == null) {
            return true;
        }
        if (isUnlimited(user)) {
            return false;
        }
        // 主管即使 data_scope 被误配成 4，也应能看本部门，避免"主管看不到下属"的功能性问题
        return !user.hasAnyRole(RoleCode.MANAGER);
    }

    /** 是否为本部门维度（含下级） */
    public static boolean isDeptScoped(LoginUser user) {
        if (user == null || isUnlimited(user)) {
            return false;
        }
        return user.getDataScope().isDeptScoped();
    }

    /** 当前用户所属部门 ID */
    public static Long deptId(LoginUser user) {
        return user == null ? null : user.getDeptId();
    }

    /** 当前用户关联的员工 ID */
    public static Long employeeId(LoginUser user) {
        return user == null ? null : user.getEmployeeId();
    }

    /** 自定义数据范围涉及的部门 ID 集合（data_scope = 5） */
    public static List<Long> customDeptIds(LoginUser user) {
        if (user == null) {
            return List.of();
        }
        DataScopeType scope = user.getDataScope();
        if (scope == DataScopeType.CUSTOM) {
            List<Long> ids = user.getScopeDeptIds();
            return ids == null ? List.of() : ids;
        }
        return List.of();
    }

    /**
     * 判断目标员工是否在当前用户可见范围内。
     * 用于详情接口、审核接口等"按 ID 直查"场景的越权拦截
     * ——列表可以靠 WHERE 过滤，按 ID 直查必须显式校验，否则改个 URL 就能越权。
     *
     * @param user             当前登录用户
     * @param targetEmployeeId 目标员工 ID
     * @param targetDeptId     目标员工所属部门 ID（可为 null）
     */
    public static boolean canAccessEmployee(LoginUser user, Long targetEmployeeId, Long targetDeptId) {
        if (user == null) {
            return false;
        }
        if (isUnlimited(user)) {
            return true;
        }
        if (isSelfOnly(user)) {
            return targetEmployeeId != null && targetEmployeeId.equals(user.getEmployeeId());
        }
        // 部门维度：同部门即可（下级部门的可见性由调用方按需要扩展）
        return targetDeptId != null && targetDeptId.equals(user.getDeptId());
    }
}
