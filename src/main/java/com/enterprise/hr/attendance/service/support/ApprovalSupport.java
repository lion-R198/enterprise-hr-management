package com.enterprise.hr.attendance.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.BizType;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.notice.entity.ApprovalRecord;
import com.enterprise.hr.notice.entity.TodoTask;
import com.enterprise.hr.notice.mapper.ApprovalRecordMapper;
import com.enterprise.hr.notice.mapper.TodoTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批公共逻辑支持（请假 / 加班 / 出差三张单据共用一套审批语义）。
 *
 * <p>为什么抽出来：三张单据的审批链、入参（{@code result} / {@code opinion}）、
 * 留痕字段、待办生成规则完全一致，只有 {@code biz_type} 不同。
 * 若在各 Service 内各写一份，一旦规则微调（如新增审批层级）就会出现三处不一致。
 *
 * <p>覆盖的规则与流程：
 * <ul>
 *     <li>审批结果合法性：只接受 2 通过 / 3 驳回</li>
 *     <li>审批人权限：{@code current_approver_id == 当前用户员工ID} 的主管，或人事专员</li>
 *     <li>写入 {@code approval_record} 留痕（含审批人姓名快照）</li>
 *     <li>生成 / 完结 {@code todo_task} 待办</li>
 *     <li>解析审批人：直属主管 →（无主管时）本部门负责人 →（仍无）人事兜底</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class ApprovalSupport {

    /** 审批结果：通过 */
    private static final int RESULT_APPROVED = 2;
    /** 审批结果：驳回 */
    private static final int RESULT_REJECTED = 3;
    /** 待办优先级：中 */
    private static final int TODO_PRIORITY_MEDIUM = 2;
    /** 人事兜底部门名称关键字 */
    private static final String[] HR_DEPT_KEYWORDS = {"人力资源", "人事"};

    private final ApprovalRecordMapper approvalRecordMapper;
    private final TodoTaskMapper todoTaskMapper;
    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;
    private final AttendanceEmployeeSupport employeeInfoSupport;

    /**
     * 校验审批结果取值。
     *
     * @return 归一化后的审批结果（2 或 3）
     */
    public int parseResult(Integer result) {
        if (result == null || (result != RESULT_APPROVED && result != RESULT_REJECTED)) {
            throw BizException.badRequest("无效的审批结果");
        }
        return result;
    }

    /**
     * 校验当前用户是否有权审批该单据。
     *
     * <p>权限口径：当前审批人本人（且具备部门主管角色）或人事专员 / 管理员。
     * 这里必须显式校验而不是只靠类级 {@code @RequireRoles}：
     * 类级注解只能保证"是个主管角色"，无法保证"审批的是派给自己的单"。
     */
    public void checkApprovePermission(LoginUser user, Long currentApproverId) {
        boolean isManager = user.hasAnyRole(RoleCode.MANAGER)
                && currentApproverId != null
                && currentApproverId.equals(user.getEmployeeId());
        boolean isHr = user.hasAnyRole(RoleCode.HR, RoleCode.ADMIN);
        if (!isManager && !isHr) {
            throw BizException.forbidden("无审批权限（仅直属主管或人事专员）");
        }
    }

    /**
     * 写入审批留痕 {@code approval_record}。
     *
     * <p>审批人姓名做<b>快照</b>落库：员工改名或离职后，历史留痕仍应显示当时的审批人。
     */
    public void saveApprovalRecord(BizType bizType, Long bizId, String bizNo,
                                   int result, String opinion, LoginUser user) {
        ApprovalRecord record = new ApprovalRecord();
        record.setBizType(bizType.getValue());
        record.setBizId(bizId);
        record.setBizNo(bizNo);
        record.setApprovalLevel(1);
        record.setApproverId(user.getEmployeeId());
        record.setApproverName(resolveApproverName(user));
        record.setApprovalStatus(result == RESULT_APPROVED
                ? BizType.APPROVAL_APPROVED : BizType.APPROVAL_REJECTED);
        record.setApprovalOpinion(opinion == null ? "" : opinion);
        record.setApprovalTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    /**
     * 给审批人生成待办 {@code todo_task}（task_type = 1 待审批）。
     *
     * <p>接收人为空时不生成：宁可没有待办，也不要落一条"无人认领"的脏数据。
     */
    public void createTodo(Long receiverId, String title, String content,
                           BizType bizType, Long bizId, String bizNo) {
        if (receiverId == null) {
            return;
        }
        TodoTask todo = new TodoTask();
        todo.setReceiverId(receiverId);
        todo.setTaskType(BizConst.TODO_TYPE_APPROVAL);
        todo.setTaskTitle(title);
        todo.setTaskContent(content);
        todo.setBizType(bizType.getValue());
        todo.setBizId(bizId);
        todo.setBizNo(bizNo);
        todo.setPriority(TODO_PRIORITY_MEDIUM);
        todo.setHandleStatus(BizConst.TODO_PENDING);
        todoTaskMapper.insert(todo);
    }

    /**
     * 单据办结后把对应待办标记为已处理，避免审批人的待办列表残留已完成事项。
     */
    public void finishTodo(BizType bizType, Long bizId) {
        if (bizId == null) {
            return;
        }
        todoTaskMapper.update(null, Wrappers.<TodoTask>lambdaUpdate()
                .set(TodoTask::getHandleStatus, BizConst.TODO_HANDLED)
                .set(TodoTask::getHandleTime, LocalDateTime.now())
                .eq(TodoTask::getBizType, bizType.getValue())
                .eq(TodoTask::getBizId, bizId)
                .eq(TodoTask::getHandleStatus, BizConst.TODO_PENDING));
    }

    /**
     * 解析单据的当前审批人。
     *
     * <p>兜底顺序：直属主管 → 本部门负责人（不能是申请人本人）→ 人事部门在职人员。
     * 全部落空时抛 400 而不是静默落 null：没有审批人的单据会永久卡在"审批中"，
     * 属于必须让提交人当场知道的数据配置问题。
     */
    public Long resolveApprover(Employee applicant) {
        if (applicant == null) {
            throw BizException.badRequest("申请人不存在，无法确定审批人");
        }
        Long applicantId = applicant.getId();

        // 1) 直属主管
        Long approverId = applicant.getManagerId();
        if (isValidApprover(approverId, applicantId)) {
            return approverId;
        }
        // 2) 本部门负责人
        approverId = employeeInfoSupport.deptLeaderId(applicant.getDeptId());
        if (isValidApprover(approverId, applicantId)) {
            return approverId;
        }
        // 3) 人事兜底
        approverId = resolveHrFallback(applicantId);
        if (approverId != null) {
            return approverId;
        }
        throw BizException.badRequest("未找到审批人，请先为该员工配置直属主管");
    }

    /**
     * 单据状态校验：单据必须处于"审批中"（{@code approval_status = 1}）才允许审批。
     *
     * <p>三张单据同款校验，抽在此处保证提示语一致。
     */
    public void requirePending(Integer approvalStatus) {
        if (approvalStatus == null || approvalStatus != BizConst.APPROVAL_PENDING) {
            throw BizException.badRequest("该单据不在审批中状态");
        }
    }

    // ---------------- 内部辅助 ----------------

    private String resolveApproverName(LoginUser user) {
        String name = employeeInfoSupport.nameOf(user.getEmployeeId());
        return StringUtils.hasText(name) ? name : user.getRealName();
    }

    private boolean isValidApprover(Long approverId, Long applicantId) {
        return approverId != null && !approverId.equals(applicantId);
    }

    /**
     * 人事兜底：找名称含"人力资源 / 人事"的部门，优先取其在职负责人，
     * 否则取该部门任一位在职员工。
     */
    private Long resolveHrFallback(Long applicantId) {
        for (String keyword : HR_DEPT_KEYWORDS) {
            List<Dept> depts = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                    .like(Dept::getName, keyword)
                    .eq(Dept::getStatus, BizConst.STATUS_ENABLED));
            for (Dept dept : depts) {
                Long leaderId = dept.getLeaderId();
                if (isValidApprover(leaderId, applicantId) && isOnDuty(leaderId)) {
                    return leaderId;
                }
                List<Employee> candidates = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                        .select(Employee::getId)
                        .eq(Employee::getDeptId, dept.getId())
                        .eq(Employee::getStatus, BizConst.EMPLOYEE_ACTIVE)
                        .orderByAsc(Employee::getId));
                for (Employee candidate : candidates) {
                    if (isValidApprover(candidate.getId(), applicantId)) {
                        return candidate.getId();
                    }
                }
            }
        }
        return null;
    }

    private boolean isOnDuty(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        return employee != null
                && employee.getStatus() != null
                && employee.getStatus() == BizConst.EMPLOYEE_ACTIVE;
    }
}
