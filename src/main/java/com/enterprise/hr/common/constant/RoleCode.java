package com.enterprise.hr.common.constant;

/**
 * 角色编码常量。
 *
 * <p>与 {@code sys_role.role_code} 及前端路由 {@code meta.roles} 矩阵同源，
 * 三处必须保持一致，否则会出现"菜单可见但接口 403"的不一致体验。
 *
 * @author Enterprise HR Team
 */
public final class RoleCode {

    /** 系统管理员：全部数据范围 + 系统设置 */
    public static final String ADMIN = "ROLE_ADMIN";
    /** 人事专员：全员档案、考勤、薪资、招聘、培训、绩效 */
    public static final String HR = "ROLE_HR";
    /** 部门主管：本部门及下级数据 */
    public static final String MANAGER = "ROLE_MANAGER";
    /** 财务人员：薪资核算与发放 */
    public static final String FINANCE = "ROLE_FINANCE";
    /** 普通员工：仅本人数据 */
    public static final String EMPLOYEE = "ROLE_EMPLOYEE";
    /** 决策层：全局只读看板 */
    public static final String EXECUTIVE = "ROLE_EXECUTIVE";

    private RoleCode() {
    }
}
