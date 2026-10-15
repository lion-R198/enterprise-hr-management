package com.enterprise.hr.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.recruit.dto.CandidateCreateReq;
import com.enterprise.hr.recruit.dto.CandidateQuery;
import com.enterprise.hr.recruit.dto.CandidateStageReq;
import com.enterprise.hr.recruit.dto.CandidateVO;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.entity.Candidate;

/**
 * 候选人业务接口（F-06-03）。
 *
 * @author Enterprise HR Team
 */
public interface CandidateService extends IService<Candidate> {

    /** 候选人分页（可按需求、阶段、人才库、姓名筛选） */
    PageResult<CandidateVO> pageCandidates(CandidateQuery query);

    /** 录入候选人（初始阶段：待筛选），返回 {@code {success,id}} */
    IdResultVO create(CandidateCreateReq req);

    /** 推进候选人阶段（BR-06-01 阶段单向流转） */
    void changeStage(Long id, CandidateStageReq req);
}
