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
import java.time.LocalDateTime;

/**
 * AttendanceMonth实体（对应表 {@code attendance_month}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("attendance_month")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AttendanceMonth implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 所属部门ID(冗余,便于部门维度统计) */
    private Long deptId;

    /** 统计年份 */
    private Integer recordYear;

    /** 统计月份:1-12 */
    private Integer recordMonth;

    /** 应出勤天数 */
    private BigDecimal shouldDays;

    /** 实际出勤天数 */
    private BigDecimal actualDays;

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyCount;

    /** 旷工次数 */
    private Integer absentCount;

    /** 缺卡次数 */
    private Integer missingCount;

    /** 请假天数合计 */
    private BigDecimal leaveDays;

    /** 加班时长合计(小时) */
    private BigDecimal overtimeHours;

    /** 出差天数合计 */
    private BigDecimal tripDays;

    /** 封账状态:0未封账 1已封账(禁改) */
    private Integer lockStatus;

    /** 封账时间 */
    private LocalDateTime lockTime;

    /** 封账操作人用户ID */
    private Long lockBy;

    /** 备注 */
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
