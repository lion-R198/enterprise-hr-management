package com.enterprise.hr.recruit.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.recruit.dto.RecruitApprovalReq;
import com.enterprise.hr.recruit.dto.RecruitRequestCreateReq;
import com.enterprise.hr.recruit.dto.RecruitRequestQuery;
import com.enterprise.hr.recruit.dto.RecruitRequestVO;
import com.enterprise.hr.recruit.dto.RequestResultVO;
import com.enterprise.hr.recruit.dto.SimpleResultVO;
import com.enterprise.hr.recruit.service.RecruitRequestService;
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
 * 招聘需求接口（F-06-01 分页 / F-06-02 提报与审批）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/recruit-requests")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class RecruitRequestController {

    private final RecruitRequestService recruitRequestService;

    /** 招聘需求分页（按单号倒序，补需求部门名与提报人姓名） */
    @GetMapping("/page")
    public R<PageResult<RecruitRequestVO>> page(@Valid RecruitRequestQuery query) {
        return R.ok(recruitRequestService.pageRequests(query));
    }

    /** 提报招聘需求（BR-06-02：候选人阶段不占编制，主管与人事可提报） */
    @PostMapping
    @RequireRoles({RoleCode.HR, RoleCode.MANAGER})
    @OperLog(module = "招聘管理", type = OperType.INSERT, action = "提报招聘需求")
    public R<RequestResultVO> create(@Valid @RequestBody RecruitRequestCreateReq req) {
        return R.ok(recruitRequestService.create(req));
    }

    /** 招聘需求审批（2 通过 / 3 驳回，仅人事可操作） */
    @PutMapping("/{id}/approve")
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "招聘管理", type = OperType.APPROVE, action = "招聘需求审批")
    public R<SimpleResultVO> approve(@PathVariable("id") Long id,
                                     @Valid @RequestBody RecruitApprovalReq req) {
        recruitRequestService.approve(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
