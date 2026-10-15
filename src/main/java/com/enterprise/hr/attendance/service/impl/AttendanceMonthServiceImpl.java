package com.enterprise.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.attendance.dto.AttendanceMonthQuery;
import com.enterprise.hr.attendance.dto.AttendanceMonthVO;
import com.enterprise.hr.attendance.entity.AttendanceMonth;
import com.enterprise.hr.attendance.mapper.AttendanceMonthMapper;
import com.enterprise.hr.attendance.service.AttendanceMonthService;
import com.enterprise.hr.attendance.service.support.AttendanceEmployeeSupport;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤月报业务实现（F-04-06）。
 *
 * <p>落地规则：<b>BR-04-03</b> 已封账的月报禁止再次封账；
 * 薪资核算（BR-05-02）依赖"当月月报全部封账"这一前置条件，因此封账动作只开放给人事。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class AttendanceMonthServiceImpl extends ServiceImpl<AttendanceMonthMapper, AttendanceMonth>
        implements AttendanceMonthService {

    private final AttendanceEmployeeSupport employeeInfoSupport;

    @Override
    public PageResult<AttendanceMonthVO> pageMonths(AttendanceMonthQuery query) {
        LoginUser user = UserContext.require();
        LambdaQueryWrapper<AttendanceMonth> wrapper = Wrappers.lambdaQuery();

        // 行级数据范围（NF-10）：attendance_month 自带 dept_id 冗余列，可直接按部门过滤
        if (!DataScopeHelper.isUnlimited(user)) {
            if (DataScopeHelper.isSelfOnly(user)) {
                wrapper.eq(AttendanceMonth::getEmployeeId, user.getEmployeeId());
            } else {
                wrapper.eq(AttendanceMonth::getDeptId, user.getDeptId());
            }
        }
        if (query.getYear() != null) {
            wrapper.eq(AttendanceMonth::getRecordYear, query.getYear());
        }
        if (query.getMonth() != null) {
            wrapper.eq(AttendanceMonth::getRecordMonth, query.getMonth());
        }
        if (query.getLockStatus() != null) {
            wrapper.eq(AttendanceMonth::getLockStatus, query.getLockStatus());
        }
        wrapper.orderByDesc(AttendanceMonth::getRecordYear)
                .orderByDesc(AttendanceMonth::getRecordMonth)
                .orderByAsc(AttendanceMonth::getEmployeeId);

        Page<AttendanceMonth> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<AttendanceMonthVO> records = new ArrayList<>(page.getRecords().size());
        for (AttendanceMonth month : page.getRecords()) {
            AttendanceMonthVO vo = new AttendanceMonthVO();
            BeanUtils.copyProperties(month, vo);
            records.add(vo);
        }
        employeeInfoSupport.fillEmployeeFields(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** BR-04-03：已封账的月报禁止重复封账（封账后禁改） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long id) {
        LoginUser user = UserContext.require();

        AttendanceMonth month = baseMapper.selectById(id);
        if (month == null) {
            throw BizException.notFound("月报不存在");
        }
        if (month.getLockStatus() != null && month.getLockStatus() == BizConst.MONTH_LOCKED) {
            throw new BizException(ErrorCode.MONTH_LOCKED);
        }

        month.setLockStatus(BizConst.MONTH_LOCKED);
        month.setLockTime(LocalDateTime.now());
        // lock_by 存的是"操作人用户ID"，与列注释一致（不是员工ID）
        month.setLockBy(user.getUserId());
        baseMapper.updateById(month);
    }
}
