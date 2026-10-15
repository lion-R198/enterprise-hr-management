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
 * BusinessTrip实体（对应表 {@code business_trip}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("business_trip")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessTrip implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申请单号(唯一),如 BT20260901001 */
    private String requestNo;

    /** 申请人员工ID */
    private Long employeeId;

    /** 出差目的地 */
    private String destination;

    /** 出差事由 */
    private String purpose;

    /** 出差开始时间 */
    private LocalDateTime startTime;

    /** 出差结束时间 */
    private LocalDateTime endTime;

    /** 出差天数 */
    private BigDecimal days;

    /** 交通方式:1飞机 2高铁 3汽车 4自驾 5其他 */
    private Integer transport;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销 */
    private Integer approvalStatus;

    /** 当前审批人员工ID */
    private Long currentApproverId;

    /** 申请时间 */
    private LocalDateTime applyTime;

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
