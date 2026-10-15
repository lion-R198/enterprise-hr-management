package com.enterprise.hr.training.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.training.dto.TrainingPlanCreateReq;
import com.enterprise.hr.training.dto.TrainingPlanCreateVO;
import com.enterprise.hr.training.dto.TrainingPlanQuery;
import com.enterprise.hr.training.dto.TrainingPlanVO;
import com.enterprise.hr.training.entity.TrainingPlan;

/**
 * 培训计划业务接口（F-07-01 计划管理 / F-07-02 培训报名）。
 *
 * @author Enterprise HR Team
 */
public interface TrainingPlanService extends IService<TrainingPlan> {

    /** 培训计划分页（补组织人姓名、已报名人数、关联课程名） */
    PageResult<TrainingPlanVO> pagePlans(TrainingPlanQuery query);

    /** 新增培训计划（仅人事），返回 {@code {success,id,planCode}} */
    TrainingPlanCreateVO create(TrainingPlanCreateReq req);

    /** 员工报名（BR-07-01 满员自动关闭） */
    void enroll(Long id);
}
