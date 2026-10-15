package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.LeaveRequestCreateReq;
import com.enterprise.hr.attendance.dto.LeaveRequestQuery;
import com.enterprise.hr.attendance.dto.LeaveRequestVO;
import com.enterprise.hr.attendance.dto.SimpleResultVO;
import com.enterprise.hr.attendance.service.LeaveService;
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
 * 请假申请接口（F-04-02）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/leave-requests")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class LeaveRequestController {

    private final LeaveService leaveService;

    /** 请假单分页 */
    @GetMapping("/page")
    public R<PageResult<LeaveRequestVO>> page(@Valid LeaveRequestQuery query) {
        return R.ok(leaveService.pageRequests(query));
    }

    /** 提交请假申请（BR-04-01 重叠校验、BR-04-02 余额校验） */
    @PostMapping
    @OperLog(module = "考勤管理", type = OperType.INSERT, action = "提交请假申请")
    public R<CreateResultVO> create(@Valid @RequestBody LeaveRequestCreateReq req) {
        return R.ok(leaveService.create(req));
    }

    /** 请假审批（2 通过 / 3 驳回） */
    @PutMapping("/{id}/approve")
    @OperLog(module = "考勤管理", type = OperType.APPROVE, action = "请假审批")
    public R<SimpleResultVO> approve(@PathVariable("id") Long id,
                                     @Valid @RequestBody ApprovalReq req) {
        leaveService.approve(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
