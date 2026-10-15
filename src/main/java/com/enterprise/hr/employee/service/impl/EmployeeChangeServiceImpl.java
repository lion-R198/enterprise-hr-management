package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.employee.dto.EmployeeChangeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeChangeQuery;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.EmployeeChange;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.EmployeeChangeMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.employee.service.EmployeeChangeService;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工异动业务实现。
 *
 * <p>异动"生效即改主数据"：组织架构调整要求员工档案立即反映新部门/新岗位，
 * 若只写异动记录而主数据不动，列表页会与实际组织脱节。
 * 记录与主数据更新在同一事务内，避免"记录有了、人没动"的中间态。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class EmployeeChangeServiceImpl extends ServiceImpl<EmployeeChangeMapper, EmployeeChange>
        implements EmployeeChangeService {

    private final EmployeeMapper employeeMapper;
    private final PositionMapper positionMapper;
    private final EmployeeInfoSupport employeeInfoSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeChangeCreateReq req) {
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        if (req.getEmployeeId() == null) {
            throw BizException.badRequest("缺少必填字段：employeeId");
        }
        if (req.getChangeType() == null) {
            throw BizException.badRequest("缺少必填字段：changeType");
        }
        if (req.getEffectDate() == null) {
            throw BizException.badRequest("缺少必填字段：effectDate");
        }
        Employee employee = employeeMapper.selectById(req.getEmployeeId());
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + req.getEmployeeId());
        }

        // 未传的"异动后"字段视为不变，直接沿用现值，保证快照完整
        Long afterDeptId = req.getAfterDeptId() == null ? employee.getDeptId() : req.getAfterDeptId();
        BigDecimal afterSalary = req.getAfterSalary() == null ? employee.getSalary() : req.getAfterSalary();
        // BR-02-02：薪资不得为负数
        if (afterSalary != null && afterSalary.signum() < 0) {
            throw new BizException(ErrorCode.NEGATIVE_SALARY);
        }
        String afterPosition = employee.getPosition();
        if (req.getAfterPositionId() != null) {
            Position position = positionMapper.selectById(req.getAfterPositionId());
            if (position == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在: " + req.getAfterPositionId());
            }
            afterPosition = position.getPositionName();
        }

        EmployeeChange change = new EmployeeChange();
        change.setEmployeeId(employee.getId());
        change.setChangeType(req.getChangeType());
        change.setBeforeDeptId(employee.getDeptId());
        change.setAfterDeptId(afterDeptId);
        change.setBeforePosition(employee.getPosition());
        change.setAfterPosition(afterPosition);
        change.setBeforeSalary(employee.getSalary());
        change.setAfterSalary(afterSalary);
        change.setEffectDate(req.getEffectDate());
        change.setReason(req.getReason() == null ? "" : req.getReason());
        change.setApprovalStatus(BizConst.APPROVAL_APPROVED);
        change.setOperatorId(UserContext.currentEmployeeId());
        change.setRemark(req.getRemark());
        baseMapper.insert(change);

        syncEmployee(employee.getId(), req, afterDeptId, afterPosition, afterSalary);
        return change.getId();
    }

    @Override
    public PageResult<EmployeeChangeVO> page(EmployeeChangeQuery query) {
        EmployeeChangeQuery q = query == null ? new EmployeeChangeQuery() : query;

        LambdaQueryWrapper<EmployeeChange> wrapper = Wrappers.<EmployeeChange>lambdaQuery();
        if (q.getEmployeeId() != null) {
            wrapper.eq(EmployeeChange::getEmployeeId, q.getEmployeeId());
        }
        if (q.getChangeType() != null) {
            wrapper.eq(EmployeeChange::getChangeType, q.getChangeType());
        }
        // 生效日期倒序：最新的异动排在最前，便于查看最近履历
        wrapper.orderByDesc(EmployeeChange::getEffectDate).orderByDesc(EmployeeChange::getId);

        IPage<EmployeeChange> page = baseMapper.selectPage(q.toPage(), wrapper);
        List<EmployeeChangeVO> vos = new ArrayList<>();
        if (page.getRecords() != null) {
            for (EmployeeChange change : page.getRecords()) {
                vos.add(EmployeeChangeVO.from(change));
            }
        }
        employeeInfoSupport.fillChangeFields(vos);
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 把异动结果写回员工主数据。
     *
     * <p>只更新本次真正发生变化的字段：未传的字段保持原值，
     * 避免"办理调岗把薪资写成 null"这类静默数据损坏。
     */
    private void syncEmployee(Long employeeId, EmployeeChangeCreateReq req,
                              Long afterDeptId, String afterPosition, BigDecimal afterSalary) {
        boolean needSync = req.getAfterDeptId() != null
                || req.getAfterPositionId() != null
                || req.getAfterSalary() != null;
        if (!needSync) {
            return;
        }
        Employee patch = new Employee();
        patch.setId(employeeId);
        if (req.getAfterDeptId() != null) {
            patch.setDeptId(afterDeptId);
        }
        if (req.getAfterPositionId() != null) {
            patch.setPositionId(req.getAfterPositionId());
            patch.setPosition(afterPosition);
        }
        if (req.getAfterSalary() != null) {
            patch.setSalary(afterSalary);
        }
        patch.setUpdatedBy(UserContext.currentUserId());
        employeeMapper.updateById(patch);
    }
}
