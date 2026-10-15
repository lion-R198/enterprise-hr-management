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
 * LeaveBalance实体（对应表 {@code leave_balance}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("leave_balance")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LeaveBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 假别ID */
    private Long leaveTypeId;

    /** 所属年度,如 2026 */
    private Integer balanceYear;

    /** 年度总额度(天) */
    private BigDecimal totalDays;

    /** 上年结转天数 */
    private BigDecimal carryOverDays;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 剩余可用天数 */
    private BigDecimal remainDays;

    /** 额度失效日期 */
    private LocalDate expireDate;

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
