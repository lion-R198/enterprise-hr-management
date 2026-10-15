package com.enterprise.hr.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.attendance.dto.AttendanceMonthQuery;
import com.enterprise.hr.attendance.dto.AttendanceMonthVO;
import com.enterprise.hr.attendance.entity.AttendanceMonth;
import com.enterprise.hr.common.PageResult;

/**
 * 考勤月报业务接口（F-04-06）。
 *
 * <p>涉及业务规则：BR-04-03 封账后禁改。
 *
 * @author Enterprise HR Team
 */
public interface AttendanceMonthService extends IService<AttendanceMonth> {

    /** 月报分页（主管仅本部门，员工仅本人，人事不受限） */
    PageResult<AttendanceMonthVO> pageMonths(AttendanceMonthQuery query);

    /** 封账（BR-04-03），封账后该月数据禁改 */
    void lock(Long id);
}
