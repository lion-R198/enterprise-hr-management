package com.enterprise.hr.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.dto.EmployeeChangeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeChangeQuery;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.entity.EmployeeChange;

/**
 * 员工异动业务接口（F-02-03 人事异动）。
 *
 * <p>异动记录同时承担"履历"与"审计留痕"两个职责：
 * 入职（BR-02-01）、状态变更（BR-02-04）与调岗/调部门/晋升都在这里落一条记录。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeChangeService extends IService<EmployeeChange> {

    /**
     * 办理异动。
     *
     * <p>必填 {@code employeeId / changeType / effectDate}；
     * 记录变更前后的部门 / 职位 / 薪资快照，<b>生效即更新员工主数据</b>
     * （{@code dept_id} / {@code position_id} + {@code position} 快照 / {@code salary}）。
     *
     * @return 新异动记录 ID
     */
    Long create(EmployeeChangeCreateReq req);

    /** 异动分页（按生效日期倒序），补员工姓名与异动前后部门名 */
    PageResult<EmployeeChangeVO> page(EmployeeChangeQuery query);
}
