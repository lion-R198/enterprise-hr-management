package com.enterprise.hr.training.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训计划分页查询入参（F-07-01，{@code GET /training-plans/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingPlanQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 状态：1 草稿 / 2 已发布 / 3 进行中 / 4 已完成 / 5 已取消；空则不筛选 */
    private Integer status;

    /** 培训类型：1 新员工 / 2 专业技能 / 3 管理能力 / 4 通用素质 / 5 合规培训；空则不筛选 */
    private Integer trainType;
}
