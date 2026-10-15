package com.enterprise.hr.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.BusinessTripCreateReq;
import com.enterprise.hr.attendance.dto.BusinessTripQuery;
import com.enterprise.hr.attendance.dto.BusinessTripVO;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.entity.BusinessTrip;
import com.enterprise.hr.common.PageResult;

/**
 * 出差业务接口（F-04-04）。
 *
 * @author Enterprise HR Team
 */
public interface BusinessTripService extends IService<BusinessTrip> {

    /** 出差单分页（可见性与请假一致） */
    PageResult<BusinessTripVO> pageRequests(BusinessTripQuery query);

    /** 提交出差申请（生成待办） */
    CreateResultVO create(BusinessTripCreateReq req);

    /** 审批出差单（2 通过 / 3 驳回） */
    void approve(Long id, ApprovalReq req);
}
