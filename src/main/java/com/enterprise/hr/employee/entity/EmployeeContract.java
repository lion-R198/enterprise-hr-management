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
 * EmployeeContract实体（对应表 {@code employee_contract}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee_contract")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeContract implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 合同编号(唯一) */
    private String contractNo;

    /** 员工ID */
    private Long employeeId;

    /** 合同类型:1固定期限 2无固定期限 3以完成任务为期限 4实习协议 5劳务协议 */
    private Integer contractType;

    /** 甲方(公司名称) */
    private String firstParty;

    /** 乙方(员工姓名) */
    private String secondParty;

    /** 签订日期 */
    private LocalDate signDate;

    /** 合同生效日期 */
    private LocalDate startDate;

    /** 合同到期日期(NULL=无固定期限) */
    private LocalDate endDate;

    /** 合同期限(年) */
    private BigDecimal termYears;

    /** 合同约定薪资 */
    private BigDecimal salaryAgreed;

    /** 试用期月数 */
    private Integer probationMonths;

    /** 续签来源合同ID(上一份合同) */
    private Long renewFromId;

    /** 合同状态:1生效中 2已到期 3已续签 4已解除 5已终止 */
    private Integer status;

    /** 合同扫描件地址 */
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
