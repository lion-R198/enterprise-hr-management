package com.enterprise.hr.recruit.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 候选人分页查询入参（F-06-03，{@code GET /candidates/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CandidateQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 关联招聘需求ID；空则不筛选 */
    private Long recruitRequestId;

    /** 所处阶段：1 待筛选 / 2 待面试 / 3 面试中 / 4 待录用 / 5 已录用 / 6 已淘汰；空则不筛选 */
    private Integer stage;

    /** 是否人才库：1 是 / 0 否；空则不筛选 */
    private Integer isTalent;

    /** 关键字：按候选人姓名模糊匹配 */
    private String keyword;
}
