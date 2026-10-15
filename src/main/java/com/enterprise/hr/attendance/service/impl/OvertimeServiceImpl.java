package com.enterprise.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.OvertimeRequestCreateReq;
import com.enterprise.hr.attendance.dto.OvertimeRequestQuery;
import com.enterprise.hr.attendance.dto.OvertimeRequestVO;
import com.enterprise.hr.attendance.entity.OvertimeRequest;
import com.enterprise.hr.attendance.mapper.OvertimeRequestMapper;
import com.enterprise.hr.attendance.service.OvertimeService;
import com.enterprise.hr.attendance.service.support.ApprovalSupport;
import com.enterprise.hr.attendance.service.support.AttendanceEmployeeSupport;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.BizType;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 加班业务实现（F-04-03）。
 *
 * <p>可见性口径与请假一致（员工仅本人 / 主管本部门 / 人事不受限），
 * 审批流程与留痕复用 {@link ApprovalSupport}，避免三套单据各写一份。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class OvertimeServiceImpl extends ServiceImpl<OvertimeRequestMapper, OvertimeRequest>
        implements OvertimeService {

    /** 单号前缀：加班 */
    private static final String NO_PREFIX = "OT";
    /** 默认加班类型：工作日 */
    private static final int DEFAULT_OVERTIME_TYPE = 1;
    /** 默认补偿方式：调休 */
    private static final int DEFAULT_COMPENSATE_TYPE = 1;

    private final ApprovalSupport approvalSupport;
    private final AttendanceEmployeeSupport employeeInfoSupport;

    @Override
    public PageResult<OvertimeRequestVO> pageRequests(OvertimeRequestQuery query) {
        LoginUser user = UserContext.require();
        LambdaQueryWrapper<OvertimeRequest> wrapper = Wrappers.lambdaQuery();

        List<Long> scope = employeeInfoSupport.resolveEmployeeScope(user);
        boolean managerAll = Boolean.TRUE.equals(query.getAll())
                && user.hasAnyRole(RoleCode.MANAGER)
                && !DataScopeHelper.isSelfOnly(user);
        if (!managerAll && scope != null) {
            if (scope.isEmpty()) {
                return PageResult.empty(query.safeCurrent(), query.safeSize());
            }
            wrapper.in(OvertimeRequest::getEmployeeId, scope);
        }
        if (Boolean.TRUE.equals(query.getMine())) {
            wrapper.eq(OvertimeRequest::getEmployeeId, user.getEmployeeId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(OvertimeRequest::getApprovalStatus, query.getStatus());
        }
        wrapper.orderByDesc(OvertimeRequest::getOvertimeDate).orderByDesc(OvertimeRequest::getId);

        Page<OvertimeRequest> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<OvertimeRequestVO> records = new ArrayList<>(page.getRecords().size());
        for (OvertimeRequest request : page.getRecords()) {
            OvertimeRequestVO vo = new OvertimeRequestVO();
            BeanUtils.copyProperties(request, vo);
            records.add(vo);
        }
        employeeInfoSupport.fillEmployeeFields(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResultVO create(OvertimeRequestCreateReq req) {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        LocalTime startTime = parseTime(req.getStartTime(), "startTime");
        LocalTime endTime = parseTime(req.getEndTime(), "endTime");
        if (endTime.isBefore(startTime)) {
            throw BizException.badRequest("结束时间不得早于开始时间");
        }

        Employee applicant = employeeInfoSupport.require(employeeId);
        Long approverId = approvalSupport.resolveApprover(applicant);

        OvertimeRequest entity = new OvertimeRequest();
        entity.setRequestNo(nextRequestNo());
        entity.setEmployeeId(employeeId);
        entity.setOvertimeDate(req.getOvertimeDate());
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setHours(req.getHours());
        entity.setOvertimeType(req.getOvertimeType() == null ? DEFAULT_OVERTIME_TYPE : req.getOvertimeType());
        entity.setCompensateType(req.getCompensateType() == null ? DEFAULT_COMPENSATE_TYPE : req.getCompensateType());
        entity.setReason(req.getReason());
        entity.setApprovalStatus(BizConst.APPROVAL_PENDING);
        entity.setCurrentApproverId(approverId);
        entity.setApplyTime(LocalDateTime.now());
        baseMapper.insert(entity);

        approvalSupport.createTodo(approverId,
                String.format("加班审批：%s（%s %s 小时）", applicant.getName(), req.getOvertimeDate(),
                        plain(req.getHours())),
                entity.getRequestNo(), BizType.OVERTIME, entity.getId(), entity.getRequestNo());

        return new CreateResultVO(Boolean.TRUE, entity.getId(), entity.getRequestNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalReq req) {
        LoginUser user = UserContext.require();
        int result = approvalSupport.parseResult(req.getResult());

        OvertimeRequest request = baseMapper.selectById(id);
        if (request == null) {
            throw BizException.notFound("单据不存在");
        }
        approvalSupport.requirePending(request.getApprovalStatus());
        approvalSupport.checkApprovePermission(user, request.getCurrentApproverId());

        // 用 LambdaUpdateWrapper 才能把 current_approver_id 真正置空（updateById 会忽略 null 字段）
        baseMapper.update(null, Wrappers.<OvertimeRequest>lambdaUpdate()
                .set(OvertimeRequest::getApprovalStatus, result)
                .set(OvertimeRequest::getCurrentApproverId, null)
                .eq(OvertimeRequest::getId, request.getId()));

        approvalSupport.saveApprovalRecord(BizType.OVERTIME, request.getId(), request.getRequestNo(),
                result, req.getOpinion(), user);
        approvalSupport.finishTodo(BizType.OVERTIME, request.getId());
    }

    // ---------------- 内部辅助 ----------------

    /**
     * 宽松解析加班起止时间，兼容前端 {@code el-time-select} 的 {@code HH:mm}
     * 与接口文档里的 {@code HH:mm:ss} 两种写法。
     */
    static LocalTime parseTime(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw BizException.badRequest("缺少必填字段：" + fieldName);
        }
        String[] parts = raw.trim().split(":");
        if (parts.length < 2 || parts.length > 3) {
            throw BizException.badRequest("时间格式不正确：" + raw);
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            int second = parts.length == 3 ? Integer.parseInt(parts[2].trim()) : 0;
            return LocalTime.of(hour, minute, second);
        } catch (RuntimeException e) {
            throw BizException.badRequest("时间格式不正确：" + raw);
        }
    }

    private String nextRequestNo() {
        Long count = baseMapper.selectCount(Wrappers.<OvertimeRequest>lambdaQuery()
                .likeRight(OvertimeRequest::getRequestNo, BizNoGenerator.yearPrefix(NO_PREFIX)));
        return BizNoGenerator.next(NO_PREFIX, (count == null ? 0L : count) + 1);
    }

    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案");
        }
        return employeeId;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
