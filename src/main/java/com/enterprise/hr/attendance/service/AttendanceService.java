package com.enterprise.hr.attendance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.attendance.dto.AttendanceRecordQuery;
import com.enterprise.hr.attendance.dto.AttendanceRecordVO;
import com.enterprise.hr.attendance.dto.CheckinResultVO;
import com.enterprise.hr.attendance.dto.CheckoutResultVO;
import com.enterprise.hr.attendance.dto.TodayAttendanceVO;
import com.enterprise.hr.attendance.entity.AttendanceRecord;
import com.enterprise.hr.common.PageResult;

/**
 * 考勤打卡业务接口（F-04-01）。
 *
 * @author Enterprise HR Team
 */
public interface AttendanceService extends IService<AttendanceRecord> {

    /** 今日考勤状态：返回 {@code { record, shift }}，无记录时 record 为 null */
    TodayAttendanceVO today();

    /** 上班打卡 */
    CheckinResultVO checkin();

    /** 下班打卡 */
    CheckoutResultVO checkout();

    /** 打卡记录分页 */
    PageResult<AttendanceRecordVO> pageRecords(AttendanceRecordQuery query);
}
