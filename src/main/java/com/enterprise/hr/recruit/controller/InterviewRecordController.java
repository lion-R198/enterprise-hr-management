package com.enterprise.hr.recruit.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.dto.InterviewRecordCreateReq;
import com.enterprise.hr.recruit.dto.InterviewRecordQuery;
import com.enterprise.hr.recruit.dto.InterviewRecordVO;
import com.enterprise.hr.recruit.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试记录接口（F-06-04 安排面试与录入评价）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/interview-records")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class InterviewRecordController {

    private final InterviewService interviewService;

    /** 面试记录列表（非分页，按面试时间倒序） */
    @GetMapping
    public R<PageResult<InterviewRecordVO>> list(@Valid InterviewRecordQuery query) {
        return R.ok(interviewService.listRecords(query));
    }

    /** 安排面试 / 录入评价（轮次自动递增，结论联动候选人阶段） */
    @PostMapping
    @OperLog(module = "招聘管理", type = OperType.INSERT, action = "录入面试记录")
    public R<IdResultVO> create(@Valid @RequestBody InterviewRecordCreateReq req) {
        return R.ok(interviewService.create(req));
    }
}
