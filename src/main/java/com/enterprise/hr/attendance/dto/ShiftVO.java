package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 班次响应 VO。
 *
 * <p>为什么不直接返回 {@code Shift} 实体：实体类未标注 {@code @JsonNaming}，
 * 序列化后是 {@code shiftName} 驼峰，而前端 {@code attendance/checkin.vue} 读取的是
 * {@code shift.shift_name} / {@code shift.work_start} / {@code shift.late_tolerance}。
 * 因此必须在 VO 层做一次显式的蛇形命名声明，保证接口契约稳定。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShiftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 班次编码，如 STD */
    private String shiftCode;

    /** 班次名称，如 标准班 */
    private String shiftName;

    /** 上班时间 HH:mm:ss */
    private String workStart;

    /** 下班时间 HH:mm:ss */
    private String workEnd;

    /** 休息时长（分钟） */
    private Integer restMinutes;

    /** 标准工时（小时） */
    private BigDecimal standardHours;

    /** 迟到容忍（分钟），超出计迟到 */
    private Integer lateTolerance;

    /** 早退容忍（分钟） */
    private Integer earlyTolerance;
}
