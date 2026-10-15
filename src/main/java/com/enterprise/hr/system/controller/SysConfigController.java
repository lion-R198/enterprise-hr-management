package com.enterprise.hr.system.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.system.dto.SysConfigQuery;
import com.enterprise.hr.system.dto.SysConfigUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysConfigVO;
import com.enterprise.hr.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统参数接口（F-09-05）。
 *
 * <p>只读开放给 HR：参数值（封账日、迟到容忍分钟数等）会被 HR 的日常作业直接感知，
 * 让他们能看到"系统当前的规则"能显著减少"为什么这次算迟到"的沟通成本。
 * 修改动作仍限定管理员。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "系统管理-参数配置", description = "参数列表 / 修改参数值")
@RestController
@RequestMapping("/sys/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /** 参数列表（可按分组筛选） */
    @Operation(summary = "参数列表")
    @GetMapping
    @RequireRoles({RoleCode.ADMIN, RoleCode.HR})
    public R<PageResult<SysConfigVO>> list(SysConfigQuery query) {
        return R.ok(sysConfigService.list(query));
    }

    /** 修改参数值（含值类型与业务区间校验） */
    @Operation(summary = "修改参数")
    @PutMapping("/{id}")
    @RequireRoles(RoleCode.ADMIN)
    @OperLog(module = "系统设置", action = "修改系统参数", type = OperType.UPDATE)
    public R<OpResultVO> update(@PathVariable Long id, @Valid @RequestBody SysConfigUpdateReq req) {
        return R.ok(sysConfigService.update(id, req));
    }
}
