package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.AttendanceMonthQuery;
import com.enterprise.hr.attendance.dto.AttendanceMonthVO;
import com.enterprise.hr.attendance.dto.SimpleResultVO;
import com.enterprise.hr.attendance.service.AttendanceMonthService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考勤月报接口（F-04-06，BR-04-03 封账后禁改）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/attendance-months")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class AttendanceMonthController {

    private final AttendanceMonthService attendanceMonthService;

    /** 月报分页 */
    @GetMapping("/page")
    public R<PageResult<AttendanceMonthVO>> page(@Valid AttendanceMonthQuery query) {
        return R.ok(attendanceMonthService.pageMonths(query));
    }

    /** 封账：方法级注解收窄为仅人事可操作（BR-04-03） */
    @PutMapping("/{id}/lock")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "考勤管理", type = OperType.UPDATE, action = "考勤月报封账")
    public R<SimpleResultVO> lock(@PathVariable("id") Long id) {
        attendanceMonthService.lock(id);
        return R.ok(SimpleResultVO.ok());
    }
}
