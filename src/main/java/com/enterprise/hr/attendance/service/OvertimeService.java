package com.enterprise.hr.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.OvertimeRequestCreateReq;
import com.enterprise.hr.attendance.dto.OvertimeRequestQuery;
import com.enterprise.hr.attendance.dto.OvertimeRequestVO;
import com.enterprise.hr.attendance.entity.OvertimeRequest;
import com.enterprise.hr.common.PageResult;

/**
 * 加班业务接口（F-04-03）。
 *
 * @author Enterprise HR Team
 */
public interface OvertimeService extends IService<OvertimeRequest> {

    /** 加班单分页（可见性与请假一致） */
    PageResult<OvertimeRequestVO> pageRequests(OvertimeRequestQuery query);

    /** 提交加班申请（生成待办） */
    CreateResultVO create(OvertimeRequestCreateReq req);

    /** 审批加班单（2 通过 / 3 驳回） */
    void approve(Long id, ApprovalReq req);
}
