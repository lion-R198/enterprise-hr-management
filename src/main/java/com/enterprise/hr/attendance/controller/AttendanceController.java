package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.AttendanceRecordQuery;
import com.enterprise.hr.attendance.dto.AttendanceRecordVO;
import com.enterprise.hr.attendance.dto.CheckinResultVO;
import com.enterprise.hr.attendance.dto.CheckoutResultVO;
import com.enterprise.hr.attendance.dto.TodayAttendanceVO;
import com.enterprise.hr.attendance.service.AttendanceService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考勤打卡接口（F-04-01）。
 *
 * <p>URL 前缀 {@code /api} 由 {@code server.servlet.context-path} 提供，类内只声明业务路径。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** 今日考勤状态：{@code { record, shift }}，无记录时 record 为 null */
    @GetMapping("/today")
    public R<TodayAttendanceVO> today() {
        return R.ok(attendanceService.today());
    }

    /** 上班打卡 */
    @PostMapping("/checkin")
    @OperLog(module = "考勤管理", type = OperType.INSERT, action = "上班打卡")
    public R<CheckinResultVO> checkin() {
        return R.ok(attendanceService.checkin());
    }

    /** 下班打卡 */
    @PostMapping("/checkout")
    @OperLog(module = "考勤管理", type = OperType.INSERT, action = "下班打卡")
    public R<CheckoutResultVO> checkout() {
        return R.ok(attendanceService.checkout());
    }

    /** 打卡记录分页（普通员工只能看本人，行级范围在 Service 内收敛） */
    @GetMapping("/records")
    public R<PageResult<AttendanceRecordVO>> records(@Valid AttendanceRecordQuery query) {
        return R.ok(attendanceService.pageRecords(query));
    }
}
