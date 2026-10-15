package com.enterprise.hr.employee.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.dto.EmployeeContractCreateReq;
import com.enterprise.hr.employee.dto.EmployeeContractQuery;
import com.enterprise.hr.employee.dto.vo.ContractCreateResultVO;
import com.enterprise.hr.employee.dto.vo.EmployeeContractVO;
import com.enterprise.hr.employee.entity.EmployeeContract;
import com.enterprise.hr.employee.service.EmployeeContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 劳动合同接口（F-02-05 合同管理）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/employee-contracts")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class EmployeeContractController {

    private final EmployeeContractService employeeContractService;

    /** 合同分页：补 employee_name（乙方姓名） */
    @GetMapping("/page")
    public R<PageResult<EmployeeContractVO>> page(@Valid EmployeeContractQuery query) {
        return R.ok(employeeContractService.page(query));
    }

    /** 签订 / 续签合同：合同编号 CT 前缀服务端生成，甲方取公司名、乙方取员工姓名快照 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.INSERT, action = "签订劳动合同")
    public R<ContractCreateResultVO> create(@Valid @RequestBody EmployeeContractCreateReq req) {
        EmployeeContract contract = employeeContractService.create(req);
        return R.ok(ContractCreateResultVO.ok(contract.getId(), contract.getContractNo()));
    }
}
