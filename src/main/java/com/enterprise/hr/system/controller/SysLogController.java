package com.enterprise.hr.system.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.system.dto.SysLogQuery;
import com.enterprise.hr.system.dto.vo.SysLoginLogVO;
import com.enterprise.hr.system.dto.vo.SysOperLogVO;
import com.enterprise.hr.system.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志审计接口（F-09-06）。
 *
 * <p>权限限定 {@link RoleCode#ADMIN}：日志包含全公司账号、IP 与操作轨迹，
 * 属于安全敏感数据，与角色/账号管理同级。
 *
 * <p>只有查询、没有增删改——审计数据一旦可写就失去证据效力。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "系统管理-日志审计", description = "登录日志 / 操作日志")
@RestController
@RequestMapping("/sys/logs")
@RequiredArgsConstructor
@RequireRoles(RoleCode.ADMIN)
public class SysLogController {

    private final SysLogService sysLogService;

    /** 登录日志分页 */
    @Operation(summary = "登录日志分页")
    @GetMapping("/login")
    public R<PageResult<SysLoginLogVO>> pageLoginLogs(@Valid SysLogQuery query) {
        return R.ok(sysLogService.pageLoginLogs(query));
    }

    /** 操作日志分页 */
    @Operation(summary = "操作日志分页")
    @GetMapping("/oper")
    public R<PageResult<SysOperLogVO>> pageOperLogs(@Valid SysLogQuery query) {
        return R.ok(sysLogService.pageOperLogs(query));
    }
}
