package com.enterprise.hr.training.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.training.dto.SimpleResultVO;
import com.enterprise.hr.training.dto.TrainingPlanCreateReq;
import com.enterprise.hr.training.dto.TrainingPlanCreateVO;
import com.enterprise.hr.training.dto.TrainingPlanQuery;
import com.enterprise.hr.training.dto.TrainingPlanVO;
import com.enterprise.hr.training.service.TrainingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训计划接口（F-07-01 计划管理 / F-07-02 培训报名）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/training-plans")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    /** 培训计划分页（补组织人姓名、已报名人数 enrollCount、关联课程名 courseNames） */
    @GetMapping("/page")
    public R<PageResult<TrainingPlanVO>> page(@Valid TrainingPlanQuery query) {
        return R.ok(trainingPlanService.pagePlans(query));
    }

    /** 新增培训计划（仅人事） */
    @PostMapping
    @RequireRoles(RoleCode.HR)
    @OperLog(module = "培训管理", type = OperType.INSERT, action = "新增培训计划")
    public R<TrainingPlanCreateVO> create(@Valid @RequestBody TrainingPlanCreateReq req) {
        return R.ok(trainingPlanService.create(req));
    }

    /** 报名（BR-07-01 满员自动关闭，报名人为当前登录员工） */
    @PostMapping("/{id}/enroll")
    @OperLog(module = "培训管理", type = OperType.INSERT, action = "培训报名")
    public R<SimpleResultVO> enroll(@PathVariable("id") Long id) {
        trainingPlanService.enroll(id);
        return R.ok(SimpleResultVO.ok());
    }
}
