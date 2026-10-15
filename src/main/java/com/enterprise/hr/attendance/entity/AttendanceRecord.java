package com.enterprise.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AttendanceRecord实体（对应表 {@code attendance_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("attendance_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AttendanceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 考勤日期 */
    private LocalDate attendanceDate;

    /** 排班班次ID(NULL=使用默认班次) */
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

    /** 备注(补卡原因/异常说明) */
    private String remark;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
