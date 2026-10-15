package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 今日考勤状态响应（F-04-01，{@code GET /attendance/today}）。
 *
 * <p>契约：{@code { record, shift }}；当日无打卡记录时 {@code record} 为 {@code null}
 * （前端用 {@code todayRecord?.first_check_in} 做空安全判断）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TodayAttendanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 今日打卡记录，无记录时为 null */
    private AttendanceRecordVO record;

    /** 当前生效班次（取一个启用班次），无启用班次时为 null */
    private ShiftVO shift;
}
