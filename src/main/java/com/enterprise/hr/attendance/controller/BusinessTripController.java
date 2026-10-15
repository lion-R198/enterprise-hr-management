package com.enterprise.hr.attendance.controller;

import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.BusinessTripCreateReq;
import com.enterprise.hr.attendance.dto.BusinessTripQuery;
import com.enterprise.hr.attendance.dto.BusinessTripVO;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.SimpleResultVO;
import com.enterprise.hr.attendance.service.BusinessTripService;
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
 * 出差申请接口（F-04-04）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/business-trips")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.FINANCE})
public class BusinessTripController {

    private final BusinessTripService businessTripService;

    /** 出差单分页 */
    @GetMapping("/page")
    public R<PageResult<BusinessTripVO>> page(@Valid BusinessTripQuery query) {
        return R.ok(businessTripService.pageRequests(query));
    }

    /** 提交出差申请 */
    @PostMapping
    @OperLog(module = "考勤管理", type = OperType.INSERT, action = "提交出差申请")
    public R<CreateResultVO> create(@Valid @RequestBody BusinessTripCreateReq req) {
        return R.ok(businessTripService.create(req));
    }

    /** 出差审批（2 通过 / 3 驳回） */
    @PutMapping("/{id}/approve")
    @OperLog(module = "考勤管理", type = OperType.APPROVE, action = "出差审批")
    public R<SimpleResultVO> approve(@PathVariable("id") Long id,
                                     @Valid @RequestBody ApprovalReq req) {
        businessTripService.approve(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
