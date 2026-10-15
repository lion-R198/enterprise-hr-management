package com.enterprise.hr.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.dto.InterviewRecordCreateReq;
import com.enterprise.hr.recruit.dto.InterviewRecordQuery;
import com.enterprise.hr.recruit.dto.InterviewRecordVO;
import com.enterprise.hr.recruit.entity.InterviewRecord;

/**
 * 面试记录业务接口（F-06-04）。
 *
 * @author Enterprise HR Team
 */
public interface InterviewService extends IService<InterviewRecord> {

    /** 面试记录列表（非分页，按面试时间倒序） */
    PageResult<InterviewRecordVO> listRecords(InterviewRecordQuery query);

    /** 安排面试 / 录入评价（轮次自动递增，结论联动候选人阶段），返回 {@code {success,id}} */
    IdResultVO create(InterviewRecordCreateReq req);
}
