package com.enterprise.hr.salary.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.salary.dto.SalaryCalculateReq;
import com.enterprise.hr.salary.dto.SalaryCalculateVO;
import com.enterprise.hr.salary.dto.SalaryPayReq;
import com.enterprise.hr.salary.dto.SalaryRecordQuery;
import com.enterprise.hr.salary.dto.SalaryRecordVO;
import com.enterprise.hr.salary.service.SalaryRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 月度薪资核算与发放接口（F-05-02 / F-05-03）。
 *
 * <p>类级开放给 HR / FINANCE / MANAGER / EMPLOYEE（主管与员工需查看本部门/本人数据，
 * 行级范围由 Service 的 {@code DataScopeHelper} 收敛）；核算、复核、发放三个状态变更接口
 * 用方法级注解收窄到 HR / FINANCE。
 *
 * <p>发放接口的请求体可为空（前端 {@code paySalaryRecord(id)} 不传 body），
 * 故用 {@code @RequestBody(required = false)} 接收。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/salary-records")
@RequireRoles({RoleCode.HR, RoleCode.FINANCE, RoleCode.MANAGER, RoleCode.EMPLOYEE})
@RequiredArgsConstructor
public class SalaryRecordController {

    private final SalaryRecordService salaryRecordService;

    /** 薪资记录分页（F-05-02）：GET /api/salary-records/page?current=1&size=20&year=&month=&payStatus=&deptId=&keyword= */
    @GetMapping("/page")
    public R<PageResult<SalaryRecordVO>> page(@Valid SalaryRecordQuery query) {
        return R.ok(salaryRecordService.page(query));
    }

    /** 发起月度核算：POST /api/salary-records/calculate；BR-05-02 考勤未封账时拒绝 */
    @PostMapping("/calculate")
    @RequireRoles({RoleCode.HR, RoleCode.FINANCE})
    @OperLog(module = "薪资管理", type = OperType.INSERT, action = "月度薪资核算")
    public R<SalaryCalculateVO> calculate(@Valid @RequestBody SalaryCalculateReq req) {
        int created = salaryRecordService.calculate(req);
        return R.ok(SalaryCalculateVO.builder().success(true).created(created).build());
    }

    /** 薪资复核：PUT /api/salary-records/{id}/review；BR-05-03 复核人不得与核算人相同 */
    @PutMapping("/{id}/review")
    @RequireRoles({RoleCode.HR, RoleCode.FINANCE})
    @OperLog(module = "薪资管理", type = OperType.APPROVE, action = "薪资复核")
    public R<Void> review(@PathVariable("id") Long id) {
        salaryRecordService.review(id);
        return R.ok();
    }

    /** 薪资发放：PUT /api/salary-records/{id}/pay；BR-05-04 发放后锁定 */
    @PutMapping("/{id}/pay")
    @RequireRoles({RoleCode.HR, RoleCode.FINANCE})
    @OperLog(module = "薪资管理", type = OperType.UPDATE, action = "薪资发放")
    public R<Void> pay(@PathVariable("id") Long id,
                       @Valid @RequestBody(required = false) SalaryPayReq req) {
        salaryRecordService.pay(id, req);
        return R.ok();
    }
}
