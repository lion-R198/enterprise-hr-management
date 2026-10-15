package com.enterprise.hr.common.constant;

import lombok.Getter;

/**
 * 审批业务类型：与 {@code approval_record.biz_type} 的取值一一对应。
 *
 * <p>审批流采用"多态"设计：{@code approval_record} 通过 {@code biz_type + biz_id}
 * 指向不同业务单据，不建立物理外键。本枚举是各业务模块写审批留痕时的唯一取值来源。
 *
 * @author Enterprise HR Team
 */
@Getter
public enum BizType {

    /** 请假申请 */
    LEAVE(1, "请假"),
    /** 加班申请 */
    OVERTIME(2, "加班"),
    /** 出差申请 */
    BUSINESS_TRIP(3, "出差"),
    /** 员工异动（调岗/调部门/晋升） */
    EMPLOYEE_CHANGE(4, "员工异动"),
    /** 离职申请 */
    RESIGN(5, "离职"),
    /** 招聘需求 */
    RECRUIT_REQUEST(6, "招聘需求"),
    /** 录用（Offer） */
    OFFER(7, "录用"),
    /** 调薪 */
    SALARY_ADJUST(8, "调薪");

    private final int value;
    private final String label;

    BizType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 审批结果：待审批 */
    public static final int APPROVAL_PENDING = 1;
    /** 审批结果：已同意 */
    public static final int APPROVAL_APPROVED = 2;
    /** 审批结果：已驳回 */
    public static final int APPROVAL_REJECTED = 3;
    /** 审批结果：已转交 */
    public static final int APPROVAL_TRANSFERRED = 4;
}
