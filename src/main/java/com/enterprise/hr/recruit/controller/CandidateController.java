package com.enterprise.hr.recruit.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.recruit.dto.CandidateCreateReq;
import com.enterprise.hr.recruit.dto.CandidateQuery;
import com.enterprise.hr.recruit.dto.CandidateStageReq;
import com.enterprise.hr.recruit.dto.CandidateVO;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.dto.SimpleResultVO;
import com.enterprise.hr.recruit.service.CandidateService;
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
 * 候选人接口（F-06-03 招聘漏斗）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/candidates")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class CandidateController {

    private final CandidateService candidateService;

    /** 候选人分页（补需求单号与应聘岗位名） */
    @GetMapping("/page")
    public R<PageResult<CandidateVO>> page(@Valid CandidateQuery query) {
        return R.ok(candidateService.pageCandidates(query));
    }

    /** 录入候选人（初始阶段：待筛选） */
    @PostMapping
    @OperLog(module = "招聘管理", type = OperType.INSERT, action = "录入候选人")
    public R<IdResultVO> create(@Valid @RequestBody CandidateCreateReq req) {
        return R.ok(candidateService.create(req));
    }

    /** 推进候选人阶段（BR-06-01 阶段不可回退） */
    @PutMapping("/{id}/stage")
    @OperLog(module = "招聘管理", type = OperType.UPDATE, action = "推进候选人阶段")
    public R<SimpleResultVO> changeStage(@PathVariable("id") Long id,
                                         @Valid @RequestBody CandidateStageReq req) {
        candidateService.changeStage(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
