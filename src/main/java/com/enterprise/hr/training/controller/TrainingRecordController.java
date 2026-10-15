package com.enterprise.hr.training.controller;

import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.training.dto.MyTrainingVO;
import com.enterprise.hr.training.service.TrainingArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的培训档案接口（F-07-06 员工自助查看）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class TrainingRecordController {

    private final TrainingArchiveService trainingArchiveService;

    /** 我的培训档案：{@code {records, total, enrolls, totalHours}} */
    @GetMapping("/my")
    public R<MyTrainingVO> my() {
        return R.ok(trainingArchiveService.myTraining());
    }
}
