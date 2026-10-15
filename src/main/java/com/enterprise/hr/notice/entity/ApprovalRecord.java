package com.enterprise.hr.notice.entity;

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
import java.time.LocalDateTime;

/**
 * ApprovalRecord实体（对应表 {@code approval_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("approval_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型:1请假 2加班 3出差 4员工异动 5离职 6招聘需求 7录用 8调薪 */
    private Integer bizType;

    /** 业务单据ID(多态指向不同业务表,故不设外键) */
    private Long bizId;

    /** 业务单号(冗余,便于检索) */
    private String bizNo;

    /** 审批层级,从1开始 */
    private Integer approvalLevel;

    /** 审批人员工ID */
    private Long approverId;

    /** 审批人姓名(快照) */
    private String approverName;

    /** 审批结果:1待审批 2已同意 3已驳回 4已转交 */
    private Integer approvalStatus;

    /** 审批意见 */
    private String approvalOpinion;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
