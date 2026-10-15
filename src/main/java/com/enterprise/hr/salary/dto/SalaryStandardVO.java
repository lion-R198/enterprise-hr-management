package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资标准出参 VO（F-05-01 薪资标准列表）。
 *
 * <p>响应字段一律蛇形，与 {@code salary_standard} 列名一致，
 * 前端 {@code el-table-column prop="standard_code"} 直接绑定，无需适配层。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryStandardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 标准编码（唯一） */
    private String standardCode;

    /** 标准名称 */
    private String standardName;

    /** 适用岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作 */
    private Integer positionSeq;

    /** 适用职级，如 P5/M2 */
    private String jobLevel;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位工资 */
    private BigDecimal postSalary;

    /** 绩效工资（基数） */
    private BigDecimal performanceSalary;

    /** 固定补贴合计 */
    private BigDecimal allowance;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
