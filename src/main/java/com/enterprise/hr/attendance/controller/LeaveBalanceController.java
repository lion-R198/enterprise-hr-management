package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.LeaveBalanceVO;
import com.enterprise.hr.attendance.service.LeaveService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.RequireRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 假期余额接口（BR-04-02）。
 *
 * <p>非分页的列表型接口统一用 {@code PageResult.list}，前端读 {@code records} / {@code total}。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/leave-balances")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class LeaveBalanceController {

    private final LeaveService leaveService;

    /** 我的假期余额 */
    @GetMapping("/my")
    public R<PageResult<LeaveBalanceVO>> my() {
        return R.ok(leaveService.myBalances());
    }
}
