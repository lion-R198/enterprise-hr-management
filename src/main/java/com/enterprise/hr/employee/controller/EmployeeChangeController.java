package com.enterprise.hr.employee.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.dto.EmployeeChangeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeChangeQuery;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.dto.vo.IdResultVO;
import com.enterprise.hr.employee.service.EmployeeChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工异动接口（F-02-03 人事异动）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/employee-changes")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class EmployeeChangeController {

    private final EmployeeChangeService employeeChangeService;

    /** 办理异动（调岗 / 调部门 / 晋升 / 降级）：记录变更前后快照并同步员工主数据 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.UPDATE, action = "办理员工异动")
    public R<IdResultVO> create(@Valid @RequestBody EmployeeChangeCreateReq req) {
        return R.ok(IdResultVO.ok(employeeChangeService.create(req)));
    }

    /** 异动分页：按生效日期倒序，补员工姓名与异动前后部门名 */
    @GetMapping("/page")
    public R<PageResult<EmployeeChangeVO>> page(@Valid EmployeeChangeQuery query) {
        return R.ok(employeeChangeService.page(query));
    }
}
