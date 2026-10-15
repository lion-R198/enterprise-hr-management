package com.enterprise.hr.performance.entity;

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
 * PerformanceRecord实体（对应表 {@code performance_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("performance_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformanceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考核方案ID */
    private Long planId;

    /** 被考核员工ID */
    private Long employeeId;

    /** 所属部门ID(冗余,便于部门统计) */
    private Long deptId;

    /** 考核主管员工ID */
    private Long leaderId;

    /** 自评加权总分 */
    private BigDecimal selfScore;

    /** 主管评分加权总分 */
    private BigDecimal leaderScore;

    /** 隔级复核分 */
    private BigDecimal reviewScore;

    /** 最终得分(按方案权重计算) */
    private BigDecimal finalScore;

    /** 绩效等级:S/A/B/C/D */
    private String grade;

    /** 部门内排名 */
    private Integer deptRank;

    /** 员工确认状态:0待确认 1已确认 2已申诉 */
    private Integer employeeConfirmStatus;

    /** 申诉理由 */
    private String appealReason;

    /** 申诉处理结果 */
    private String appealResult;

    /** 归档状态:0未归档 1已归档(锁定) */
    private Integer archiveStatus;

    /** 归档时间 */
    private LocalDateTime archiveTime;

    /** 流程状态:1待自评 2待主管评 3待复核 4待员工确认 5已完成 */
    private Integer status;

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
