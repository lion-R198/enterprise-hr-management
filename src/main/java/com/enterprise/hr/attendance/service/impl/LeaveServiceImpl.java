package com.enterprise.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.LeaveBalanceVO;
import com.enterprise.hr.attendance.dto.LeaveRequestCreateReq;
import com.enterprise.hr.attendance.dto.LeaveRequestQuery;
import com.enterprise.hr.attendance.dto.LeaveRequestVO;
import com.enterprise.hr.attendance.entity.LeaveBalance;
import com.enterprise.hr.attendance.entity.LeaveRequest;
import com.enterprise.hr.attendance.mapper.LeaveBalanceMapper;
import com.enterprise.hr.attendance.mapper.LeaveRequestMapper;
import com.enterprise.hr.attendance.service.LeaveService;
import com.enterprise.hr.attendance.service.support.ApprovalSupport;
import com.enterprise.hr.attendance.service.support.AttendanceEmployeeSupport;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.BizType;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.config.EhrProperties;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.LeaveType;
import com.enterprise.hr.employee.mapper.LeaveTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 请假业务实现（F-04-02）。
 *
 * <p>落地规则：
 * <ul>
 *     <li><b>BR-04-01</b> 与本人审批中/已通过的单据时间区间重叠时拒绝提交</li>
 *     <li><b>BR-04-02</b> 年假 / 事假提交前校验 {@code leave_balance.remain_days}，审批通过后回写已用与剩余天数</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements LeaveService {

    /** 单号前缀：请假 */
    private static final String NO_PREFIX = "LV";
    /** 纳入余额管控的假别编码：年假、事假 */
    private static final Set<String> BALANCE_CONTROLLED_CODES =
            Collections.unmodifiableSet(new LinkedHashSet<>(List.of("ANNUAL", "PERSONAL")));

    private final LeaveBalanceMapper leaveBalanceMapper;
    private final LeaveTypeMapper leaveTypeMapper;
    private final EhrProperties ehrProperties;
    private final ApprovalSupport approvalSupport;
    private final AttendanceEmployeeSupport employeeInfoSupport;

    @Override
    public PageResult<LeaveRequestVO> pageRequests(LeaveRequestQuery query) {
        LoginUser user = UserContext.require();
        LambdaQueryWrapper<LeaveRequest> wrapper = Wrappers.lambdaQuery();

        // 行级数据范围（NF-10）：员工仅本人、主管本部门、人事不受限
        List<Long> scope = employeeInfoSupport.resolveEmployeeScope(user);
        // 主管传 all=true 表示"看全部"；员工即使传了 all 也仍然只能看本人（越权参数不生效）
        boolean managerAll = Boolean.TRUE.equals(query.getAll())
                && user.hasAnyRole(RoleCode.MANAGER)
                && !DataScopeHelper.isSelfOnly(user);
        if (!managerAll && scope != null) {
            if (scope.isEmpty()) {
                return PageResult.empty(query.safeCurrent(), query.safeSize());
            }
            wrapper.in(LeaveRequest::getEmployeeId, scope);
        }
        // "我的申请"页签：在可见范围内进一步收窄到本人
        if (Boolean.TRUE.equals(query.getMine())) {
            wrapper.eq(LeaveRequest::getEmployeeId, user.getEmployeeId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(LeaveRequest::getApprovalStatus, query.getStatus());
        }
        wrapper.orderByDesc(LeaveRequest::getApplyTime).orderByDesc(LeaveRequest::getId);

        Page<LeaveRequest> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<LeaveRequestVO> records = new ArrayList<>(page.getRecords().size());
        for (LeaveRequest request : page.getRecords()) {
            records.add(toVO(request));
        }
        fillDisplayFields(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 提交请假申请。
     *
     * <p>规则顺序有意如此：先做 BR-04-01 重叠（与余额无关的硬冲突），
     * 再做 BR-04-02 余额（依赖假别配置），最后才写库并派发待办。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResultVO create(LeaveRequestCreateReq req) {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw BizException.badRequest("结束时间不得早于开始时间");
        }
        LeaveType leaveType = leaveTypeMapper.selectById(req.getLeaveTypeId());
        if (leaveType == null) {
            throw BizException.notFound("假别不存在");
        }

        // BR-04-01：与本人"草稿/审批中"及"已通过"的单据时间区间重叠时拒绝
        checkTimeOverlap(employeeId, req);

        // BR-04-02：年假 / 事假需校验余额
        checkBalance(employeeId, leaveType, req.getDays());

        Employee applicant = employeeInfoSupport.require(employeeId);
        Long approverId = approvalSupport.resolveApprover(applicant);

        LeaveRequest entity = new LeaveRequest();
        entity.setRequestNo(nextRequestNo());
        entity.setEmployeeId(employeeId);
        entity.setLeaveTypeId(leaveType.getId());
        entity.setStartTime(req.getStartTime());
        entity.setEndTime(req.getEndTime());
        entity.setDays(req.getDays());
        entity.setReason(req.getReason());
        entity.setApprovalStatus(BizConst.APPROVAL_PENDING);
        entity.setCurrentApproverId(approverId);
        entity.setApplyTime(LocalDateTime.now());
        baseMapper.insert(entity);

        approvalSupport.createTodo(approverId,
                String.format("请假审批：%s（%s %s 天）", applicant.getName(), leaveType.getTypeName(),
                        plain(req.getDays())),
                entity.getRequestNo(), BizType.LEAVE, entity.getId(), entity.getRequestNo());

        return new CreateResultVO(Boolean.TRUE, entity.getId(), entity.getRequestNo());
    }

    /**
     * 审批请假单。
     *
     * <p>通过时回写假期余额（BR-04-02 的另一半）：只对已存在的余额记录累加，
     * 不给未配置额度的假别凭空虚造一条余额。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalReq req) {
        LoginUser user = UserContext.require();
        int result = approvalSupport.parseResult(req.getResult());

        LeaveRequest request = baseMapper.selectById(id);
        if (request == null) {
            throw BizException.notFound("单据不存在");
        }
        approvalSupport.requirePending(request.getApprovalStatus());
        approvalSupport.checkApprovePermission(user, request.getCurrentApproverId());

        // 用 LambdaUpdateWrapper 而不是 updateById：后者默认忽略 null 字段，
        // 无法把 current_approver_id 真正置空，单据会残留"当前审批人"
        baseMapper.update(null, Wrappers.<LeaveRequest>lambdaUpdate()
                .set(LeaveRequest::getApprovalStatus, result)
                .set(LeaveRequest::getCurrentApproverId, null)
                .eq(LeaveRequest::getId, request.getId()));

        approvalSupport.saveApprovalRecord(BizType.LEAVE, request.getId(), request.getRequestNo(),
                result, req.getOpinion(), user);
        approvalSupport.finishTodo(BizType.LEAVE, request.getId());

        if (result == BizType.APPROVAL_APPROVED) {
            writeBackBalance(request);
        }
    }

    @Override
    public PageResult<LeaveBalanceVO> myBalances() {
        LoginUser user = UserContext.require();
        Long employeeId = user.getEmployeeId();
        // 契约统一：「我的 xxx」读接口在账号未绑定员工档案时返回空集合，而不是报错。
        // 纯管理/运维账号（如 admin）本就没有员工档案，打开页面看到红色报错是误导；
        // 需要以员工身份「执行动作」的接口（打卡）才返回 400 并给出可执行提示。
        // 注意此处不能走 requireEmployeeId()，那会抛 400。
        if (employeeId == null) {
            return PageResult.list(List.of());
        }
        int year = ehrProperties.getBusiness().getLeaveBalanceYear();

        List<LeaveBalance> balances = leaveBalanceMapper.selectList(Wrappers.<LeaveBalance>lambdaQuery()
                .eq(LeaveBalance::getEmployeeId, employeeId)
                .eq(LeaveBalance::getBalanceYear, year)
                .orderByAsc(LeaveBalance::getLeaveTypeId));

        Map<Long, String> typeNames = loadLeaveTypeNames();
        List<LeaveBalanceVO> records = new ArrayList<>(balances.size());
        for (LeaveBalance balance : balances) {
            LeaveBalanceVO vo = new LeaveBalanceVO();
            BeanUtils.copyProperties(balance, vo);
            vo.setLeaveTypeName(typeNames.get(balance.getLeaveTypeId()));
            records.add(vo);
        }
        // "我的 xxx"类接口统一用 PageResult.list，前端读 records / total
        return PageResult.list(records);
    }

    // ---------------- 业务规则 ----------------

    /** BR-04-01：请假时间区间不得与本人审批中/已通过的单据重叠（按日期维度比较，与前端契约一致） */
    private void checkTimeOverlap(Long employeeId, LeaveRequestCreateReq req) {
        Long overlapped = baseMapper.selectCount(Wrappers.<LeaveRequest>lambdaQuery()
                .eq(LeaveRequest::getEmployeeId, employeeId)
                .in(LeaveRequest::getApprovalStatus, BizConst.APPROVAL_DRAFT, BizConst.APPROVAL_PENDING,
                        BizConst.APPROVAL_APPROVED)
                .lt(LeaveRequest::getStartTime, req.getEndTime().toLocalDate().plusDays(1).atStartOfDay())
                .ge(LeaveRequest::getEndTime, req.getStartTime().toLocalDate().atStartOfDay()));
        if (overlapped != null && overlapped > 0) {
            throw new BizException(ErrorCode.LEAVE_TIME_OVERLAP);
        }
    }

    /** BR-04-02：年假 / 事假提交前校验剩余额度 */
    private void checkBalance(Long employeeId, LeaveType leaveType, BigDecimal days) {
        if (!BALANCE_CONTROLLED_CODES.contains(leaveType.getTypeCode())) {
            return;
        }
        LeaveBalance balance = findBalance(employeeId, leaveType.getId());
        if (balance == null || balance.getRemainDays() == null) {
            // 未配置额度记录时不拦截，避免"新员工还没建余额就完全无法请假"
            return;
        }
        if (balance.getRemainDays().compareTo(days) < 0) {
            throw new BizException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT,
                    "假期余额不足：剩余 " + plain(balance.getRemainDays()) + " 天（BR-04-02）");
        }
    }

    /** 审批通过后回写已用/剩余天数 */
    private void writeBackBalance(LeaveRequest request) {
        BigDecimal days = request.getDays();
        if (days == null || days.signum() <= 0) {
            return;
        }
        LeaveBalance balance = findBalance(request.getEmployeeId(), request.getLeaveTypeId());
        if (balance == null) {
            return;
        }
        BigDecimal used = nvl(balance.getUsedDays()).add(days);
        BigDecimal remain = nvl(balance.getRemainDays()).subtract(days);
        if (remain.signum() < 0) {
            remain = BigDecimal.ZERO;
        }
        balance.setUsedDays(used);
        balance.setRemainDays(remain);
        leaveBalanceMapper.updateById(balance);
    }

    private LeaveBalance findBalance(Long employeeId, Long leaveTypeId) {
        List<LeaveBalance> list = leaveBalanceMapper.selectList(Wrappers.<LeaveBalance>lambdaQuery()
                .eq(LeaveBalance::getEmployeeId, employeeId)
                .eq(LeaveBalance::getLeaveTypeId, leaveTypeId)
                .eq(LeaveBalance::getBalanceYear, ehrProperties.getBusiness().getLeaveBalanceYear())
                .orderByAsc(LeaveBalance::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    // ---------------- 内部辅助 ----------------

    /** 单号：LV + 年份 + 5 位流水，流水取当年已有单据数 + 1 */
    private String nextRequestNo() {
        Long count = baseMapper.selectCount(Wrappers.<LeaveRequest>lambdaQuery()
                .likeRight(LeaveRequest::getRequestNo, BizNoGenerator.yearPrefix(NO_PREFIX)));
        return BizNoGenerator.next(NO_PREFIX, (count == null ? 0L : count) + 1);
    }

    private Map<Long, String> loadLeaveTypeNames() {
        List<LeaveType> types = leaveTypeMapper.selectList(Wrappers.<LeaveType>lambdaQuery());
        Map<Long, String> map = new HashMap<>(types.size() * 2);
        for (LeaveType type : types) {
            map.put(type.getId(), type.getTypeName());
        }
        return map;
    }

    /** 补全申请人信息、假别名称、当前审批人姓名 */
    private void fillDisplayFields(List<LeaveRequestVO> records) {
        if (records.isEmpty()) {
            return;
        }
        employeeInfoSupport.fillEmployeeFields(records);

        Map<Long, String> typeNames = loadLeaveTypeNames();
        List<Long> approverIds = new ArrayList<>(records.size());
        for (LeaveRequestVO vo : records) {
            vo.setLeaveTypeName(typeNames.get(vo.getLeaveTypeId()));
            if (vo.getCurrentApproverId() != null) {
                approverIds.add(vo.getCurrentApproverId());
            }
        }
        Map<Long, Employee> approvers = employeeInfoSupport.loadEmployees(approverIds);
        for (LeaveRequestVO vo : records) {
            if (vo.getCurrentApproverId() == null) {
                vo.setCurrentApproverName(null);
                continue;
            }
            Employee approver = approvers.get(vo.getCurrentApproverId());
            vo.setCurrentApproverName(approver == null ? "#" + vo.getCurrentApproverId() : approver.getName());
        }
    }

    private static LeaveRequestVO toVO(LeaveRequest request) {
        LeaveRequestVO vo = new LeaveRequestVO();
        BeanUtils.copyProperties(request, vo);
        return vo;
    }

    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案");
        }
        return employeeId;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
