package com.enterprise.hr.common.constant;

/**
 * 全局业务常量：集中放置跨模块共用的状态值与阈值，避免魔法数字散落各层。
 *
 * @author Enterprise HR Team
 */
public final class BizConst {

    private BizConst() {
    }

    // ---------------- 通用启用状态 ----------------
    /** 停用 / 禁用 / 未删除的"否" */
    public static final int STATUS_DISABLED = 0;
    /** 启用 / 正常 */
    public static final int STATUS_ENABLED = 1;

    // ---------------- 通用布尔标志（is_top / is_default / is_system 等） ----------------
    /** 是 */
    public static final int FLAG_YES = 1;
    /** 否 */
    public static final int FLAG_NO = 0;

    // ---------------- 员工在职状态（employee.status） ----------------
    /** 离职 */
    public static final int EMPLOYEE_RESIGNED = 0;
    /** 在职 */
    public static final int EMPLOYEE_ACTIVE = 1;
    /** 试用 */
    public static final int EMPLOYEE_PROBATION = 2;
    /** 待入职 */
    public static final int EMPLOYEE_PENDING = 3;

    // ---------------- 部门类型（dept.dept_type） ----------------
    /** 集团/公司 */
    public static final int DEPT_TYPE_COMPANY = 1;
    /** 中心 */
    public static final int DEPT_TYPE_CENTER = 2;
    /** 部门 */
    public static final int DEPT_TYPE_DEPT = 3;
    /** 部门树建议最大层级（BR-03-03） */
    public static final int DEPT_MAX_LEVEL = 5;

    // ---------------- 审批状态（各业务单据 approval_status） ----------------
    /** 草稿/待提交 */
    public static final int APPROVAL_DRAFT = 0;
    /** 审批中 */
    public static final int APPROVAL_PENDING = 1;
    /** 已通过 */
    public static final int APPROVAL_APPROVED = 2;
    /** 已驳回 */
    public static final int APPROVAL_REJECTED = 3;

    // ---------------- 考勤打卡状态（attendance_record） ----------------
    /** 打卡状态：正常 */
    public static final int CHECK_NORMAL = 1;
    /** 打卡状态：迟到 / 早退 */
    public static final int CHECK_LATE = 2;
    /** 打卡状态：严重迟到 / 缺卡 */
    public static final int CHECK_SEVERE = 3;
    /** 考勤结果：正常 */
    public static final int ATTEND_NORMAL = 1;
    /** 考勤结果：迟到 */
    public static final int ATTEND_LATE = 2;
    /** 考勤结果：早退 */
    public static final int ATTEND_EARLY = 3;
    /** 考勤结果：缺卡 */
    public static final int ATTEND_MISSING = 4;
    /** 考勤结果：旷工 */
    public static final int ATTEND_ABSENT = 5;
    /** 数据来源：移动端打卡 */
    public static final int SOURCE_MOBILE = 2;

    // ---------------- 考勤月报封账（attendance_month.lock_status） ----------------
    /** 未封账 */
    public static final int MONTH_UNLOCKED = 0;
    /** 已封账 */
    public static final int MONTH_LOCKED = 1;

    // ---------------- 薪资发放状态（salary_record.pay_status） ----------------
    /** 已核算，待复核 */
    public static final int PAY_CALCULATED = 1;
    /** 已复核，待发放 */
    public static final int PAY_REVIEWED = 2;
    /** 已发放 */
    public static final int PAY_PAID = 3;

    // ---------------- 候选人阶段（candidate.stage） ----------------
    /** 待筛选 */
    public static final int CANDIDATE_NEW = 1;
    /** 简历通过 */
    public static final int CANDIDATE_SCREENED = 2;
    /** 面试中 */
    public static final int CANDIDATE_INTERVIEWING = 3;
    /** 待录用 */
    public static final int CANDIDATE_OFFER = 4;
    /** 已入职 */
    public static final int CANDIDATE_HIRED = 5;
    /** 已淘汰 */
    public static final int CANDIDATE_ELIMINATED = 6;

    // ---------------- Offer 状态（offer_record.status） ----------------
    /** 待发送 */
    public static final int OFFER_DRAFT = 1;
    /** 已发送 */
    public static final int OFFER_SENT = 2;
    /** 已接受 */
    public static final int OFFER_ACCEPTED = 3;
    /** 已拒绝 */
    public static final int OFFER_REJECTED = 4;
    /** 已过期 */
    public static final int OFFER_EXPIRED = 5;
    /** 已入职 */
    public static final int OFFER_ENTRY = 6;

    // ---------------- 绩效记录状态（performance_record.status） ----------------
    /** 待自评 */
    public static final int PERF_WAIT_SELF = 1;
    /** 待主管评分 */
    public static final int PERF_WAIT_LEADER = 2;
    /** 待复核 */
    public static final int PERF_WAIT_REVIEW = 3;
    /** 待员工确认 */
    public static final int PERF_WAIT_CONFIRM = 4;
    /** 已归档 */
    public static final int PERF_ARCHIVED = 5;
    /** 员工确认状态：未确认 */
    public static final int CONFIRM_NONE = 0;
    /** 员工确认状态：已确认 */
    public static final int CONFIRM_ACCEPTED = 1;
    /** 员工确认状态：已申诉 */
    public static final int CONFIRM_APPEALED = 2;

    // ---------------- 培训计划状态（training_plan.status） ----------------
    /** 草稿 */
    public static final int TRAINING_DRAFT = 1;
    /** 报名中 */
    public static final int TRAINING_ENROLLING = 2;
    /** 进行中 */
    public static final int TRAINING_ONGOING = 3;
    /** 已结束 */
    public static final int TRAINING_FINISHED = 4;
    /** 已取消 */
    public static final int TRAINING_CANCELED = 5;

    // ---------------- 公告状态（notice.status） ----------------
    /** 草稿 */
    public static final int NOTICE_DRAFT = 1;
    /** 已发布 */
    public static final int NOTICE_PUBLISHED = 2;
    /** 已下线 */
    public static final int NOTICE_OFFLINE = 3;

    // ---------------- 公告类型（notice.notice_type） ----------------
    /** 公司制度 */
    public static final int NOTICE_TYPE_POLICY = 1;
    /** 放假通知 */
    public static final int NOTICE_TYPE_HOLIDAY = 2;
    /** 人事公告 */
    public static final int NOTICE_TYPE_HR = 3;
    /** 活动通知 */
    public static final int NOTICE_TYPE_ACTIVITY = 4;
    /** 其他 */
    public static final int NOTICE_TYPE_OTHER = 5;
    /** 公告类型上限（用于取值校验） */
    public static final int NOTICE_TYPE_MAX = NOTICE_TYPE_OTHER;

    // ---------------- 公告发布范围（notice.target_scope） ----------------
    /** 全体 */
    public static final int NOTICE_SCOPE_ALL = 1;
    /** 指定部门 */
    public static final int NOTICE_SCOPE_DEPTS = 2;

    // ---------------- 待办任务（todo_task） ----------------
    /** 待办类型：审批 */
    public static final int TODO_TYPE_APPROVAL = 1;
    /** 待办：未处理 */
    public static final int TODO_PENDING = 0;
    /** 待办：已处理 */
    public static final int TODO_HANDLED = 1;

    // ---------------- 账号状态（sys_user.status） ----------------
    /** 正常 */
    public static final int USER_NORMAL = 1;
    /** 停用 */
    public static final int USER_DISABLED = 0;
    /** 锁定 */
    public static final int USER_LOCKED = 2;

    // ---------------- 角色类型（sys_role.role_type） ----------------
    /** 系统内置角色，不可删除、编码不可改 */
    public static final int ROLE_BUILTIN = 1;
    /** 自定义角色 */
    public static final int ROLE_CUSTOM = 2;
}
