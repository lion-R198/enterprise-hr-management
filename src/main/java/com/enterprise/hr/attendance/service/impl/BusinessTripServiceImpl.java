package com.enterprise.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.BusinessTripCreateReq;
import com.enterprise.hr.attendance.dto.BusinessTripQuery;
import com.enterprise.hr.attendance.dto.BusinessTripVO;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.entity.BusinessTrip;
import com.enterprise.hr.attendance.mapper.BusinessTripMapper;
import com.enterprise.hr.attendance.service.BusinessTripService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 出差业务实现（F-04-04）。
 *
 * <p>出差期间视为出勤（月报的 {@code trip_days} 由统计任务汇总），
 * 本类只负责单据流程：提交 → 派发待办 → 审批 → 留痕。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class BusinessTripServiceImpl extends ServiceImpl<BusinessTripMapper, BusinessTrip>
        implements BusinessTripService {

    /** 单号前缀：出差 */
    private static final String NO_PREFIX = "BT";

    private final ApprovalSupport approvalSupport;
    private final AttendanceEmployeeSupport employeeInfoSupport;

    @Override
    public PageResult<BusinessTripVO> pageRequests(BusinessTripQuery query) {
        LoginUser user = UserContext.require();
        LambdaQueryWrapper<BusinessTrip> wrapper = Wrappers.lambdaQuery();

        List<Long> scope = employeeInfoSupport.resolveEmployeeScope(user);
        boolean managerAll = Boolean.TRUE.equals(query.getAll())
                && user.hasAnyRole(RoleCode.MANAGER)
                && !DataScopeHelper.isSelfOnly(user);
        if (!managerAll && scope != null) {
            if (scope.isEmpty()) {
                return PageResult.empty(query.safeCurrent(), query.safeSize());
            }
            wrapper.in(BusinessTrip::getEmployeeId, scope);
        }
        if (Boolean.TRUE.equals(query.getMine())) {
            wrapper.eq(BusinessTrip::getEmployeeId, user.getEmployeeId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BusinessTrip::getApprovalStatus, query.getStatus());
        }
        wrapper.orderByDesc(BusinessTrip::getStartTime).orderByDesc(BusinessTrip::getId);

        Page<BusinessTrip> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<BusinessTripVO> records = new ArrayList<>(page.getRecords().size());
        for (BusinessTrip trip : page.getRecords()) {
            BusinessTripVO vo = new BusinessTripVO();
            BeanUtils.copyProperties(trip, vo);
            records.add(vo);
        }
        employeeInfoSupport.fillEmployeeFields(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResultVO create(BusinessTripCreateReq req) {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw BizException.badRequest("结束时间不得早于开始时间");
        }

        Employee applicant = employeeInfoSupport.require(employeeId);
        Long approverId = approvalSupport.resolveApprover(applicant);

        BusinessTrip entity = new BusinessTrip();
        entity.setRequestNo(nextRequestNo());
        entity.setEmployeeId(employeeId);
        entity.setDestination(req.getDestination());
        entity.setPurpose(req.getPurpose());
        entity.setStartTime(req.getStartTime());
        entity.setEndTime(req.getEndTime());
        entity.setDays(req.getDays());
        entity.setTransport(req.getTransport());
        entity.setBudgetAmount(req.getBudgetAmount() == null ? BigDecimal.ZERO : req.getBudgetAmount());
        entity.setApprovalStatus(BizConst.APPROVAL_PENDING);
        entity.setCurrentApproverId(approverId);
        entity.setApplyTime(LocalDateTime.now());
        baseMapper.insert(entity);

        approvalSupport.createTodo(approverId,
                String.format("出差审批：%s（%s %s 天）", applicant.getName(), req.getDestination(),
                        req.getDays() == null ? "0" : req.getDays().stripTrailingZeros().toPlainString()),
                entity.getRequestNo(), BizType.BUSINESS_TRIP, entity.getId(), entity.getRequestNo());

        return new CreateResultVO(Boolean.TRUE, entity.getId(), entity.getRequestNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalReq req) {
        LoginUser user = UserContext.require();
        int result = approvalSupport.parseResult(req.getResult());

        BusinessTrip trip = baseMapper.selectById(id);
        if (trip == null) {
            throw BizException.notFound("单据不存在");
        }
        approvalSupport.requirePending(trip.getApprovalStatus());
        approvalSupport.checkApprovePermission(user, trip.getCurrentApproverId());

        // 用 LambdaUpdateWrapper 才能把 current_approver_id 真正置空（updateById 会忽略 null 字段）
        baseMapper.update(null, Wrappers.<BusinessTrip>lambdaUpdate()
                .set(BusinessTrip::getApprovalStatus, result)
                .set(BusinessTrip::getCurrentApproverId, null)
                .eq(BusinessTrip::getId, trip.getId()));

        approvalSupport.saveApprovalRecord(BizType.BUSINESS_TRIP, trip.getId(), trip.getRequestNo(),
                result, req.getOpinion(), user);
        approvalSupport.finishTodo(BizType.BUSINESS_TRIP, trip.getId());
    }

    // ---------------- 内部辅助 ----------------

    private String nextRequestNo() {
        Long count = baseMapper.selectCount(Wrappers.<BusinessTrip>lambdaQuery()
                .likeRight(BusinessTrip::getRequestNo, BizNoGenerator.yearPrefix(NO_PREFIX)));
        return BizNoGenerator.next(NO_PREFIX, (count == null ? 0L : count) + 1);
    }

    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案");
        }
        return employeeId;
    }
}
