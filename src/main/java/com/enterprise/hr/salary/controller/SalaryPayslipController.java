package com.enterprise.hr.salary.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.salary.dto.PayslipDetailVO;
import com.enterprise.hr.salary.dto.SalaryRecordVO;
import com.enterprise.hr.salary.service.SalaryRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资条接口（F-05-06）。
 *
 * <p>「我的薪资条」为非分页列表接口，按契约仍用 {@code PageResult.list(...)} 返回，
 * 前端统一读 {@code records} / {@code total}。
 *
 * <p>明细接口受 NF-10 行级隔离约束：非本人时仅 HR / FINANCE / ADMIN 可查看，
 * 判定落在 Service，失败返回 403。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/salary/payslips")
@RequireRoles({RoleCode.HR, RoleCode.FINANCE, RoleCode.MANAGER, RoleCode.EMPLOYEE})
@RequiredArgsConstructor
public class SalaryPayslipController {

    private final SalaryRecordService salaryRecordService;

    /** 我的薪资条：GET /api/salary/payslips/my（当前登录人关联员工的全部记录，年月倒序） */
    @GetMapping("/my")
    public R<PageResult<SalaryRecordVO>> my() {
        return R.ok(salaryRecordService.myPayslips());
    }

    /** 薪资条明细：GET /api/salary/payslips/{recordId}/items，返回 {@code {record, items}} */
    @GetMapping("/{recordId}/items")
    public R<PayslipDetailVO> items(@PathVariable("recordId") Long recordId) {
        return R.ok(salaryRecordService.payslipItems(recordId));
    }
}
