package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录响应 VO（{@code attendance_record} 行 + 员工联表展示字段）。
 *
 * <p>响应字段一律蛇形命名，与数据库列名/前端 {@code prop} 一致；
 * 前端 {@code attendance/checkin.vue} 直接绑定 {@code first_check_in} / {@code last_check_out} 等。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AttendanceRecordVO implements Serializable, EmployeeAwareVO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名（联表展示字段） */
    private String employeeName;

    /** 员工工号（联表展示字段） */
    private String empNo;

    /** 部门名称（联表展示字段） */
    private String deptName;

    /** 考勤日期 */
    private LocalDate attendanceDate;

    /** 排班班次ID */
    private Long shiftId;

    /** 首次上班打卡时间 */
    private LocalDateTime firstCheckIn;

    /** 末次下班打卡时间 */
    private LocalDateTime lastCheckOut;

    /** 实际出勤工时(小时) */
    private BigDecimal workHours;

    /** 迟到分钟数 */
    private Integer lateMinutes;

    /** 早退分钟数 */
    private Integer earlyMinutes;

    /** 上班打卡状态:1正常 2迟到 3严重迟到 */
    private Integer checkInStatus;

    /** 下班打卡状态:1正常 2早退 3缺卡 */
    private Integer checkOutStatus;

    /** 考勤结果:1正常 2迟到 3早退 4缺卡 5旷工 6休假 7出差 8调休 */
    private Integer attendanceStatus;

    /** 数据来源:1考勤设备 2移动端打卡 3补卡审批 */
    private Integer source;

    /** 备注 */
    private String remark;
}
