package com.enterprise.hr.common.constant;

import lombok.Getter;

/**
 * 数据范围类型：与 {@code sys_role.data_scope} 的取值一一对应。
 *
 * <p>行级权限（NF-10）判定入口。用户持多个角色时取<b>最宽</b>范围生效
 * （数值最小者最宽），避免员工角色把主管角色的可见范围"收窄"。
 *
 * @author Enterprise HR Team
 */
@Getter
public enum DataScopeType {

    /** 1 全部数据 */
    ALL(1, "全部数据"),
    /** 2 本部门 */
    DEPT(2, "本部门"),
    /** 3 本部门及下级 */
    DEPT_AND_CHILD(3, "本部门及下级"),
    /** 4 仅本人 */
    SELF(4, "仅本人"),
    /** 5 自定义部门集合 */
    CUSTOM(5, "自定义部门");

    private final int value;
    private final String label;

    DataScopeType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static DataScopeType of(Integer value) {
        if (value == null) {
            return SELF;
        }
        for (DataScopeType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        // 未知取值按最保守处理，避免越权
        return SELF;
    }

    /** 是否可查看全部数据 */
    public boolean isAll() {
        return this == ALL;
    }

    /** 是否仅可查看本人数据 */
    public boolean isSelfOnly() {
        return this == SELF;
    }

    /** 是否按部门维度限制 */
    public boolean isDeptScoped() {
        return this == DEPT || this == DEPT_AND_CHILD || this == CUSTOM;
    }
}
