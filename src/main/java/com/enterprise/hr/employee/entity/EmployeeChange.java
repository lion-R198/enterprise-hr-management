package com.enterprise.hr.employee.entity;

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
 * EmployeeChange实体（对应表 {@code employee_change}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee_change")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeChange implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 异动类型:1入职 2转正 3调岗 4调部门 5晋升 6降级 7离职 8返聘 */
    private Integer changeType;

    /** 异动前部门ID */
    private Long beforeDeptId;

    /** 异动后部门ID */
    private Long afterDeptId;

    /** 异动前职位 */
    private String beforePosition;

    /** 异动后职位 */
    private String afterPosition;

    /** 异动前薪资 */
    private BigDecimal beforeSalary;

    /** 异动后薪资 */
    private BigDecimal afterSalary;

    /** 生效日期 */
    private LocalDate effectDate;

    /** 异动原因 */
    private String reason;

    /** 关联申请单号 */
    private String applyNo;

    /** 审批状态:0待提交 1审批中 2已通过 3已驳回 */
    private Integer approvalStatus;

    /** 经办人(员工ID) */
    private Long operatorId;

    /** 异动附件(调令等) */
    private String attachmentUrl;

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
