package com.enterprise.hr.system.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.system.dto.SysUserCreateReq;
import com.enterprise.hr.system.dto.SysUserQuery;
import com.enterprise.hr.system.dto.SysUserUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysUserVO;
import com.enterprise.hr.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统用户接口（F-09-01 用户管理）。
 *
 * <p>权限：整类限定 {@link RoleCode#ADMIN}。账号管理是"能给自己和他人发权限"的能力，
 * 一旦下放给其它角色就等于权限提升漏洞，因此不放宽。
 *
 * <p>接口级操作日志：只标注写接口（新增/修改/重置/删除）。查询接口不标注，
 * 否则日志表会被列表刷新淹没，失去审计价值。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "系统管理-用户", description = "账号分页 / 新增 / 修改 / 重置密码 / 删除")
@RestController
@RequestMapping("/sys/users")
@RequiredArgsConstructor
@RequireRoles(RoleCode.ADMIN)
public class SysUserController {

    private final SysUserService sysUserService;

    /** 账号分页（关键字匹配账号或姓名，可按状态过滤） */
    @Operation(summary = "账号分页")
    @GetMapping("/page")
    public R<PageResult<SysUserVO>> page(@Valid SysUserQuery query) {
        return R.ok(sysUserService.page(query));
    }

    /** 新增账号（BR-09-01：初始密码不少于 8 位） */
    @Operation(summary = "新增账号")
    @PostMapping
    @OperLog(module = "系统设置", action = "新增系统账号", type = OperType.INSERT)
    public R<OpResultVO> create(@Valid @RequestBody SysUserCreateReq req) {
        return R.ok(sysUserService.create(req));
    }

    /** 修改账号（局部更新；BR-09-02：管理员账号保护） */
    @Operation(summary = "修改账号")
    @PutMapping("/{id}")
    @OperLog(module = "系统设置", action = "修改系统账号", type = OperType.UPDATE)
    public R<OpResultVO> update(@PathVariable Long id, @RequestBody SysUserUpdateReq req) {
        return R.ok(sysUserService.update(id, req));
    }

    /** 重置密码为系统初始密码，并解除锁定 */
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password/reset")
    @OperLog(module = "系统设置", action = "重置账号密码", type = OperType.UPDATE)
    public R<OpResultVO> resetPassword(@PathVariable Long id) {
        return R.ok(sysUserService.resetPassword(id));
    }

    /** 删除账号（BR-09-02：管理员账号不可删除） */
    @Operation(summary = "删除账号")
    @DeleteMapping("/{id}")
    @OperLog(module = "系统设置", action = "删除系统账号", type = OperType.DELETE)
    public R<OpResultVO> delete(@PathVariable Long id) {
        return R.ok(sysUserService.delete(id));
    }
}
