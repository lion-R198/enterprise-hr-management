package com.enterprise.hr.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.recruit.dto.RecruitApprovalReq;
import com.enterprise.hr.recruit.dto.RecruitRequestCreateReq;
import com.enterprise.hr.recruit.dto.RecruitRequestQuery;
import com.enterprise.hr.recruit.dto.RecruitRequestVO;
import com.enterprise.hr.recruit.dto.RequestResultVO;
import com.enterprise.hr.recruit.entity.RecruitRequest;

/**
 * 招聘需求业务接口（F-06-01 / F-06-02）。
 *
 * @author Enterprise HR Team
 */
public interface RecruitRequestService extends IService<RecruitRequest> {

    /** 招聘需求分页（按单号倒序） */
    PageResult<RecruitRequestVO> pageRequests(RecruitRequestQuery query);

    /** 主管提报招聘需求（BR-06-02），返回 {@code {success,id,requestNo}} */
    RequestResultVO create(RecruitRequestCreateReq req);

    /** HR 审批招聘需求（2 通过 / 3 驳回） */
    void approve(Long id, RecruitApprovalReq req);
}
