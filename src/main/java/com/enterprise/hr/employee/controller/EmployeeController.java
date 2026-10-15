package com.enterprise.hr.employee.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeQuery;
import com.enterprise.hr.employee.dto.EmployeeStatusReq;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.dto.vo.EmployeeCreateResultVO;
import com.enterprise.hr.employee.dto.vo.SimpleResultVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工接口（F-02-01 ~ F-02-06）。
 *
 * <p>URL 前缀 {@code /api} 由 {@code server.servlet.context-path} 提供，
 * 因此控制器内只声明业务路径 {@code /employees}。
 *
 * <p>分层职责：Controller 只做参数绑定、DTO/Entity/VO 转换与响应封装，
 * 业务规则（BR-02-01/02/03/04）与行级数据范围（NF-10）一律落在 Service。
 *
 * <p>权限：类级放行 HR / 主管 / 员工（各自按数据范围看到不同行），
 * 所有增删改再收窄到 {@link RoleCode#HR}（管理员由拦截器内置放行）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/employees")
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 单参构造。
     *
     * <p>刻意不用 {@code @RequiredArgsConstructor} 注入更多依赖：既有 Controller 单测
     * 以 {@code new EmployeeController(employeeService)} 直接实例化，
     * 一旦增加构造参数就会破坏编译；需要部门名等数据时由 {@code EmployeeService} 内部注入 Mapper 完成。
     */
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ------------------------------ 新契约（F-02 员工信息）

    /** 员工分页：关键字（姓名/工号/手机号）+ 部门 + 岗位 + 状态，按工号升序 */
    @GetMapping("/page")
    public R<PageResult<EmployeeVO>> page(@Valid EmployeeQuery query) {
        return R.ok(employeeService.pageQuery(query));
    }

    /** 员工详情：补 dept_name / manager_name，NF-10 行级隔离，敏感字段脱敏 */
    @GetMapping("/{id}")
    public R<EmployeeVO> detail(@PathVariable("id") Long id) {
        return R.ok(employeeService.getDetail(id));
    }

    /** 新增员工（入职办理）：工号由服务端生成（BR-02-01），初始状态待入职，并写入入职异动 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.INSERT, action = "入职办理")
    public R<EmployeeCreateResultVO> create(@Valid @RequestBody EmployeeCreateReq req) {
        Employee employee = employeeService.create(req);
        return R.ok(EmployeeCreateResultVO.ok(employee.getId(), employee.getEmpNo()));
    }

    /** 修改员工（局部更新，未传字段保持原值；工号不可修改 BR-02-01） */
    @PutMapping("/{id}")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.UPDATE, action = "修改员工信息")
    public R<SimpleResultVO> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody EmployeeUpdateReq req) {
        employeeService.update(id, req);
        return R.ok(SimpleResultVO.ok());
    }

    /** 删除员工：BR-02-03 逻辑删除（置离职状态 + 离职日期，历史数据保留） */
    @DeleteMapping("/{id}")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.DELETE, action = "删除员工（逻辑离职）")
    public R<SimpleResultVO> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return R.ok(SimpleResultVO.ok());
    }

    /** 状态变更（离职 / 转正 / 试用）：BR-02-04 状态变更需留痕 */
    @PutMapping("/{id}/status")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.UPDATE, action = "员工状态变更")
    public R<SimpleResultVO> changeStatus(@PathVariable("id") Long id,
                                          @Valid @RequestBody EmployeeStatusReq req) {
        employeeService.changeStatus(id, req);
        return R.ok(SimpleResultVO.ok());
    }

    /** 员工异动记录（非分页列表，前端统一读 records/total） */
    @GetMapping("/{id}/changes")
    public R<PageResult<EmployeeChangeVO>> changes(@PathVariable("id") Long id) {
        return R.ok(PageResult.list(employeeService.listChanges(id)));
    }

    // ------------------------------ 兼容保留（既有前端/测试仍在用）

    /** <b>兼容保留</b>：全量员工列表（按工号升序），返回 VO 同样为蛇形字段 */
    @GetMapping
    public R<List<EmployeeVO>> list() {
        List<Employee> list = employeeService.list(
                Wrappers.<Employee>lambdaQuery().orderByAsc(Employee::getEmpNo));
        return R.ok(employeeService.toVOList(list));
    }

    /** <b>兼容保留</b>：按工号查询员工 */
    @GetMapping("/by-emp-no/{empNo}")
    public R<EmployeeVO> getByEmpNo(@PathVariable("empNo") String empNo) {
        return R.ok(employeeService.toVO(employeeService.getByEmpNo(empNo)));
    }

    /** <b>兼容保留</b>：批量删除（与单个删除同样的 BR-02-03 逻辑删除语义）。路径 /batch 优先于 /{id} 匹配 */
    @DeleteMapping("/batch")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "员工管理", type = OperType.DELETE, action = "批量删除员工（逻辑离职）")
    public R<SimpleResultVO> deleteBatch(@RequestBody List<Long> ids) {
        employeeService.deleteBatch(ids);
        return R.ok(SimpleResultVO.ok());
    }
}
