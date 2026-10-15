package com.enterprise.hr.recruit.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 招聘需求分页查询入参（F-06-01，{@code GET /recruit-requests/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecruitRequestQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 需求状态：1 进行中 / 2 已完成 / 3 已取消；空则不筛选 */
    private Integer status;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回；空则不筛选 */
    private Integer approvalStatus;
}
