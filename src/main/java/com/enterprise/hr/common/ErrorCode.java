package com.enterprise.hr.common;

/**
 * 统一错误码枚举：一处定义 code 与语义化提示，避免各层硬编码字符串。
 *
 * <p>code 取值与 HTTP 状态码对齐（200/400/401/403/404/409/500），
 * 前端 {@code request.js} 依据 {@code code} 分支处理，无需解析 message。
 *
 * @author Enterprise HR Team
 */
public enum ErrorCode {

    // ---------------- 通用 ----------------
    PARAM_INVALID(R.CODE_BAD_REQUEST, "参数校验失败"),
    BIZ_ERROR(R.CODE_BAD_REQUEST, "业务校验失败"),
    NOT_FOUND(R.CODE_NOT_FOUND, "资源不存在"),
    CONFLICT(R.CODE_CONFLICT, "数据状态已变更，请刷新后重试"),
    SYSTEM_ERROR(R.CODE_FAIL, "系统繁忙，请稍后重试"),

    // ---------------- 认证与授权 ----------------
    UNAUTHORIZED(R.CODE_UNAUTHORIZED, "登录状态已失效，请重新登录"),
    BAD_CREDENTIALS(R.CODE_BAD_REQUEST, "用户名或密码错误"),
    ACCOUNT_DISABLED(R.CODE_FORBIDDEN, "账号已停用，请联系系统管理员"),
    ACCOUNT_LOCKED(R.CODE_FORBIDDEN, "账号已锁定，请稍后重试或联系管理员"),
    FORBIDDEN(R.CODE_FORBIDDEN, "没有操作权限"),
    DATA_SCOPE_DENIED(R.CODE_FORBIDDEN, "无权访问该数据（越权访问已拦截）"),

    // ---------------- 员工与组织（BR-02 / BR-03） ----------------
    EMPLOYEE_NOT_FOUND(R.CODE_NOT_FOUND, "员工不存在"),
    EMP_NO_DUPLICATED(R.CODE_BAD_REQUEST, "工号已存在"),
    NEGATIVE_SALARY(R.CODE_BAD_REQUEST, "薪资不得为负数（BR-02-02）"),
    DEPT_NOT_FOUND(R.CODE_NOT_FOUND, "部门不存在"),
    DEPT_CODE_DUPLICATED(R.CODE_BAD_REQUEST, "部门编码已存在（BR-03-01）"),
    DEPT_NAME_DUPLICATED(R.CODE_BAD_REQUEST, "同级部门名称重复（BR-03-01）"),
    DEPT_HAS_EMPLOYEE(R.CODE_BAD_REQUEST, "部门下存在在职员工，禁止删除，请先转移人员（BR-03-02）"),
    DEPT_HAS_CHILDREN(R.CODE_BAD_REQUEST, "存在下级部门，禁止删除"),
    DEPT_LEVEL_EXCEEDED(R.CODE_BAD_REQUEST, "部门树深度不建议超过 5 层（BR-03-03）"),

    // ---------------- 考勤（BR-04） ----------------
    ATTENDANCE_DUPLICATED(R.CODE_BAD_REQUEST, "今日已打卡，请勿重复操作"),
    CHECKIN_REQUIRED(R.CODE_BAD_REQUEST, "请先完成上班打卡"),
    CHECKOUT_DUPLICATED(R.CODE_BAD_REQUEST, "今日已下班打卡"),
    LEAVE_TIME_OVERLAP(R.CODE_BAD_REQUEST, "申请时间段与已有请假单重叠（BR-04-01）"),
    LEAVE_BALANCE_INSUFFICIENT(R.CODE_BAD_REQUEST, "假期余额不足（BR-04-02）"),
    MONTH_LOCKED(R.CODE_BAD_REQUEST, "该考勤月报已封账，禁止修改（BR-04-03）"),

    // ---------------- 薪资（BR-05） ----------------
    SALARY_MONTH_UNLOCKED(R.CODE_BAD_REQUEST, "存在考勤月报未封账，禁止启动核算（BR-05-02）"),
    SALARY_STATUS_ILLEGAL(R.CODE_BAD_REQUEST, "当前薪资状态不允许该操作（BR-05-03）"),
    SALARY_REVIEWER_CONFLICT(R.CODE_BAD_REQUEST, "复核人不得与核算人相同（BR-05-03）"),
    SALARY_LOCKED(R.CODE_BAD_REQUEST, "薪资已发放并锁定，禁止修改（BR-05-04）"),

    // ---------------- 招聘（BR-06） ----------------
    CANDIDATE_STAGE_ROLLBACK(R.CODE_BAD_REQUEST, "候选人阶段不可回退（BR-06-01）"),

    // ---------------- 培训（BR-07） ----------------
    TRAINING_ENROLL_FULL(R.CODE_BAD_REQUEST, "报名人数已满，报名已关闭（BR-07-01）"),
    TRAINING_ENROLL_DUPLICATED(R.CODE_BAD_REQUEST, "您已报名该培训，请勿重复报名"),

    // ---------------- 绩效（BR-08） ----------------
    PERF_STATUS_ILLEGAL(R.CODE_BAD_REQUEST, "当前考核状态不允许该操作（BR-08-01）"),

    // ---------------- 系统设置（BR-09） ----------------
    USERNAME_DUPLICATED(R.CODE_BAD_REQUEST, "登录账号已存在"),
    PASSWORD_WEAK(R.CODE_BAD_REQUEST, "密码长度不得少于 8 位（BR-09-01）"),
    ADMIN_PROTECTED(R.CODE_BAD_REQUEST, "管理员账号受保护，不可停用或删除（BR-09-02）"),
    ROLE_CODE_DUPLICATED(R.CODE_BAD_REQUEST, "角色编码已存在"),
    BUILTIN_ROLE_IMMUTABLE(R.CODE_BAD_REQUEST, "系统内置角色的编码不可修改");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
