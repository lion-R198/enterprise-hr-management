package com.enterprise.hr.performance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考核记录出参 VO（F-08-03 评分列表）。
 *
 * <p>全字段蛇形（与 {@code performance_record} 列名一致），
 * 并补齐前端表格消费的关联展示字段：
 * {@code employee_name} / {@code emp_no} / {@code dept_name} / {@code leader_name} / {@code plan_name}。
 * 这些字段前端均以蛇形 prop 直读（{@code prop="employee_name"}），无需 {@code @JsonProperty}。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformanceRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 考核方案 ID */
    private Long planId;

    /** 考核方案名称（关联 performance_plan.plan_name） */
    private String planName;

    /** 被考核员工 ID */
    private Long employeeId;

    /** 被考核员工姓名（关联 employee.name） */
    private String employeeName;

    /** 被考核员工工号（关联 employee.emp_no） */
    private String empNo;

    /** 所属部门 ID */
    private Long deptId;

    /** 所属部门名称（关联 dept.name） */
    private String deptName;

    /** 考核主管员工 ID */
    private Long leaderId;

    /** 考核主管姓名（关联 employee.name） */
    private String leaderName;

    /** 自评加权总分 */
    private BigDecimal selfScore;

    /** 主管评分加权总分 */
    private BigDecimal leaderScore;

    /** 隔级复核分 */
    private BigDecimal reviewScore;

    /** 最终得分（按方案权重计算） */
    private BigDecimal finalScore;

    /** 绩效等级：S/A/B/C/D */
    private String grade;

    /** 部门内排名 */
    private Integer deptRank;

    /** 员工确认状态：0 待确认 / 1 已确认 / 2 已申诉 */
    private Integer employeeConfirmStatus;

    /** 申诉理由 */
    private String appealReason;

    /** 申诉处理结果 */
    private String appealResult;

    /** 归档状态：0 未归档 / 1 已归档 */
    private Integer archiveStatus;

    /** 归档时间 */
    private LocalDateTime archiveTime;

    /** 流程状态：1 待自评 / 2 待主管评 / 3 待复核 / 4 待员工确认 / 5 已完成 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
