package com.enterprise.hr.employee.controller;

import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.dto.DeptSaveReq;
import com.enterprise.hr.employee.dto.vo.DeptTreeVO;
import com.enterprise.hr.employee.dto.vo.IdResultVO;
import com.enterprise.hr.employee.dto.vo.SimpleResultVO;
import com.enterprise.hr.employee.service.DeptService;
import lombok.RequiredArgsConstructor;
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
 * 部门接口（F-03-01 组织架构 / F-03-02 部门维护 / F-03-04 编制占用）。
 *
 * <p>业务规则：BR-03-01 编码唯一与同级名称唯一、BR-03-02 有在职员工禁止删除、
 * BR-03-03 层级不超过 5 层 —— 全部落在 {@link DeptService}。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class DeptController {

    private final DeptService deptService;

    /** 组织架构树（非分页，直接返回列表；每个节点带 employeeCount） */
    @GetMapping("/tree")
    public R<List<DeptTreeVO>> tree() {
        return R.ok(deptService.tree());
    }

    /** 部门扁平列表（仅启用状态），供员工/岗位/招聘表单下拉框使用 */
    @GetMapping
    public R<List<DeptTreeVO>> list() {
        return R.ok(deptService.listEnabled());
    }

    /** 新增部门：BR-03-01 编码唯一、BR-03-03 层级不超过 5 层 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "部门管理", type = OperType.INSERT, action = "新增部门")
    public R<IdResultVO> create(@RequestBody DeptSaveReq req) {
        return R.ok(IdResultVO.ok(deptService.create(req)));
    }

    /** 修改部门（局部更新）：BR-03-01 同级名称不可重复；禁止挂到自己的子孙下 */
    @PutMapping("/{id}")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "部门管理", type = OperType.UPDATE, action = "修改部门")
    public R<SimpleResultVO> update(@PathVariable("id") Long id, @RequestBody DeptSaveReq req) {
        deptService.update(id, req);
        return R.ok(SimpleResultVO.ok());
    }

    /** 删除部门：有下级部门或 BR-03-02 有在职员工时拒绝 */
    @DeleteMapping("/{id}")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "部门管理", type = OperType.DELETE, action = "删除部门")
    public R<SimpleResultVO> delete(@PathVariable("id") Long id) {
        deptService.delete(id);
        return R.ok(SimpleResultVO.ok());
    }
}
