package com.enterprise.hr.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.attendance.dto.ApprovalReq;
import com.enterprise.hr.attendance.dto.CreateResultVO;
import com.enterprise.hr.attendance.dto.LeaveBalanceVO;
import com.enterprise.hr.attendance.dto.LeaveRequestCreateReq;
import com.enterprise.hr.attendance.dto.LeaveRequestQuery;
import com.enterprise.hr.attendance.dto.LeaveRequestVO;
import com.enterprise.hr.attendance.entity.LeaveRequest;
import com.enterprise.hr.common.PageResult;

/**
 * 请假业务接口（F-04-02）。
 *
 * <p>涉及业务规则：BR-04-01 请假时间区间不得重叠、BR-04-02 假期余额校验与回写。
 *
 * @author Enterprise HR Team
 */
public interface LeaveService extends IService<LeaveRequest> {

    /** 请假单分页（员工仅本人，主管看本部门，all=true 时看全部） */
    PageResult<LeaveRequestVO> pageRequests(LeaveRequestQuery query);

    /** 提交请假申请（含重叠校验、余额校验、生成待办） */
    CreateResultVO create(LeaveRequestCreateReq req);

    /** 审批请假单（2 通过 / 3 驳回） */
    void approve(Long id, ApprovalReq req);

    /** 我的假期余额 */
    PageResult<LeaveBalanceVO> myBalances();
}
