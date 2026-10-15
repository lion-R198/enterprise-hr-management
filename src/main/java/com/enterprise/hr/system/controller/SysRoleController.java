package com.enterprise.hr.system.controller;

import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.system.dto.SysRoleSaveReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysRoleVO;
import com.enterprise.hr.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 角色权限接口（F-09-02）。
 *
 * <p>类级权限为 {@link RoleCode#ADMIN}，但 {@code GET /sys/roles} 是例外：
 * 用户管理页（{@code system/user.vue}）新增账号时需要角色下拉选项，
 * 若列表也要求管理员角色则该页面无法渲染。列表只暴露角色编码与名称，
 * 不含任何敏感信息，下放到 HR 可见是安全且必要的。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "系统管理-角色", description = "角色列表 / 新增 / 修改")
@RestController
@RequestMapping("/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /** 角色列表（用户管理页角色下拉选项与角色页共用） */
    @Operation(summary = "角色列表")
    @GetMapping
    @RequireRoles({RoleCode.ADMIN, RoleCode.HR})
    public R<PageResult<SysRoleVO>> list(PageQuery query) {
        return R.ok(sysRoleService.list(query));
    }

    /** 新增角色 */
    @Operation(summary = "新增角色")
    @PostMapping
    @RequireRoles(RoleCode.ADMIN)
    @OperLog(module = "系统设置", action = "新增角色", type = OperType.INSERT)
    public R<OpResultVO> create(@Valid @RequestBody SysRoleSaveReq req) {
        return R.ok(sysRoleService.create(req));
    }

    /** 修改角色（系统内置角色编码不可变更） */
    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @RequireRoles(RoleCode.ADMIN)
    @OperLog(module = "系统设置", action = "修改角色", type = OperType.UPDATE)
    public R<OpResultVO> update(@PathVariable Long id, @RequestBody SysRoleSaveReq req) {
        return R.ok(sysRoleService.update(id, req));
    }
}
