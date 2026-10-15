package com.enterprise.hr.salary.controller;

import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.entity.SalaryStandard;
import com.enterprise.hr.salary.dto.SalaryStandardCreateReq;
import com.enterprise.hr.salary.dto.SalaryStandardCreateVO;
import com.enterprise.hr.salary.dto.SalaryStandardVO;
import com.enterprise.hr.salary.service.SalaryStandardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资标准接口（F-05-01）。
 *
 * <p>URL 前缀 {@code /api} 由 {@code server.servlet.context-path} 提供，
 * 控制器内只声明业务路径 {@code /salary-standards}。
 *
 * <p>分层职责：Controller 只做参数绑定、DTO/Entity/VO 转换与响应封装，
 * 唯一性与默认值口径等业务规则落在 Service。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/salary-standards")
@RequireRoles({RoleCode.HR, RoleCode.FINANCE, RoleCode.MANAGER, RoleCode.EMPLOYEE})
@RequiredArgsConstructor
public class SalaryStandardController {

    private final SalaryStandardService salaryStandardService;

    /** 薪资标准分页（F-05-01）：GET /api/salary-standards/page?current=1&size=10 */
    @GetMapping("/page")
    public R<PageResult<SalaryStandardVO>> page(@Valid PageQuery query) {
        return R.ok(salaryStandardService.page(query));
    }

    /** 新增薪资标准：POST /api/salary-standards；{@code standard_code} 重复返回 409 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "薪资管理", type = OperType.INSERT, action = "新增薪资标准")
    public R<SalaryStandardCreateVO> create(@Valid @RequestBody SalaryStandardCreateReq req) {
        SalaryStandard standard = salaryStandardService.create(req);
        return R.ok(SalaryStandardCreateVO.builder()
                .success(true)
                .id(standard.getId())
                .standardCode(standard.getStandardCode())
                .build());
    }
}
