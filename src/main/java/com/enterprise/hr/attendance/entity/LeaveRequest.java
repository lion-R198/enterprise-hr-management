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
 * LeaveRequest实体（对应表 {@code leave_request}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("leave_request")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LeaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申请单号(唯一),如 LV20260901001 */
    private String requestNo;

    /** 申请人员工ID */
    private Long employeeId;

    /** 假别ID */
    private Long leaveTypeId;

    /** 请假开始时间 */
    private LocalDateTime startTime;

    /** 请假结束时间 */
    private LocalDateTime endTime;

    /** 请假天数(支持0.5天) */
    private BigDecimal days;

    /** 请假事由 */
    private String reason;

    /** 证明材料地址(病假条等) */
    private String attachmentUrl;

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
