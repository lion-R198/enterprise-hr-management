package com.enterprise.hr.employee.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.employee.dto.PositionQuery;
import com.enterprise.hr.employee.dto.PositionSaveReq;
import com.enterprise.hr.employee.dto.vo.IdResultVO;
import com.enterprise.hr.employee.dto.vo.PositionVO;
import com.enterprise.hr.employee.dto.vo.SimpleResultVO;
import com.enterprise.hr.employee.service.PositionService;
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
 * 岗位接口（F-03-03 岗位维护）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class PositionController {

    private final PositionService positionService;

    /** 岗位分页：补 dept_name（部门为空时为「通用」）与在编人数 employeeCount */
    @GetMapping
    public R<PageResult<PositionVO>> page(@Valid PositionQuery query) {
        return R.ok(positionService.page(query));
    }

    /** 新增岗位：编码与名称必填，编码全局唯一 */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "岗位管理", type = OperType.INSERT, action = "新增岗位")
    public R<IdResultVO> create(@RequestBody PositionSaveReq req) {
        return R.ok(IdResultVO.ok(positionService.create(req)));
    }

    /** 修改岗位（局部更新，未传字段保持原值） */
    @PutMapping("/{id}")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "岗位管理", type = OperType.UPDATE, action = "修改岗位")
    public R<SimpleResultVO> update(@PathVariable("id") Long id, @RequestBody PositionSaveReq req) {
        positionService.update(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
