package com.enterprise.hr.recruit.entity;

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
 * OfferRecord实体（对应表 {@code offer_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("offer_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OfferRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Offer编号(唯一) */
    private String offerNo;

    /** 候选人ID */
    private Long candidateId;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 录用部门ID */
    private Long deptId;

    /** 录用岗位ID */
    private Long positionId;

    /** 定级 */
    private String jobLevel;

    /** 转正后月薪 */
    private BigDecimal offerSalary;

    /** 试用期月数 */
    private Integer probationMonths;

    /** 试用期月薪(通常为转正的80%) */
    private BigDecimal probationSalary;

    /** 预计入职日期 */
    private LocalDate entryDate;

    /** 审批状态:0草稿 1审批中 2已通过 3已驳回 */
    private Integer approvalStatus;

    /** 当前审批人员工ID */
    private Long currentApproverId;

    /** Offer发送时间 */
    private LocalDateTime sendTime;

    /** 候选人确认时间 */
    private LocalDateTime confirmTime;

    /** Offer文件地址 */
    private String offerUrl;

    /** 状态:1待发送 2已发送 3已接受 4已拒绝 5已过期 6已入职 */
    private Integer status;

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
