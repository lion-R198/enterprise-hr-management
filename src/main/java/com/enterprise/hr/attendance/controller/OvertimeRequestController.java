package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.OvertimeRequestCreateReq;
import com.enterprise.hr.attendance.dto.OvertimeRequestQuery;
import com.enterprise.hr.attendance.dto.OvertimeRequestVO;
import com.enterprise.hr.attendance.dto.SimpleResultVO;
import com.enterprise.hr.attendance.service.OvertimeService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
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
 * 加班申请接口（F-04-03）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/overtime-requests")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class OvertimeRequestController {

    private final OvertimeService overtimeService;

    /** 加班单分页 */
    @GetMapping("/page")
    public R<PageResult<OvertimeRequestVO>> page(@Valid OvertimeRequestQuery query) {
        return R.ok(overtimeService.pageRequests(query));
    }

    /** 提交加班申请 */
    @PostMapping
    @OperLog(module = "考勤管理", type = OperType.INSERT, action = "提交加班申请")
    public R<CreateResultVO> create(@Valid @RequestBody OvertimeRequestCreateReq req) {
        return R.ok(overtimeService.create(req));
    }

    /** 加班审批（2 通过 / 3 驳回） */
    @PutMapping("/{id}/approve")
    @OperLog(module = "考勤管理", type = OperType.APPROVE, action = "加班审批")
    public R<SimpleResultVO> approve(@PathVariable("id") Long id,
                                     @Valid @RequestBody ApprovalReq req) {
        overtimeService.approve(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
