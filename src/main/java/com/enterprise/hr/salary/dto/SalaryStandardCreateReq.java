package com.enterprise.hr.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增薪资标准请求体（前端 {@code salary/standard.vue} 提交）。
 *
 * <p>请求体字段一律驼峰（{@code standardCode}），因此本类<b>不</b>标
 * {@code @JsonNaming}——全局蛇形策略会作用于反序列化，导致驼峰请求体绑不上。
 *
 * <p>默认值口径与前端一致：{@code positionSeq} 缺省 2（技术序列）、
 * 各类金额缺省 0.00、{@code effectiveDate} 缺省当天。
 *
 * @author Enterprise HR Team
 */
@Data
public class SalaryStandardCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标准编码（唯一），如 STD-TECH-P6 */
    @NotBlank(message = "标准编码不能为空")
    @Size(max = 32, message = "标准编码长度不得超过 32")
    private String standardCode;

    /** 标准名称，如 P6技术岗标准 */
    @NotBlank(message = "标准名称不能为空")
    @Size(max = 64, message = "标准名称长度不得超过 64")
    private String standardName;

    /** 适用岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作，缺省 2 */
    @Min(value = 1, message = "岗位序列取值为 1-4")
    @Max(value = 4, message = "岗位序列取值为 1-4")
    private Integer positionSeq;

    /** 适用职级，如 P5/M2 */
    @Size(max = 16, message = "职级长度不得超过 16")
    private String jobLevel;

    /** 基本工资 */
    @DecimalMin(value = "0", message = "基本工资不得为负数")
    private BigDecimal baseSalary;

    /** 岗位工资 */
    @DecimalMin(value = "0", message = "岗位工资不得为负数")
    private BigDecimal postSalary;

    /** 绩效工资（基数） */
    @DecimalMin(value = "0", message = "绩效工资不得为负数")
    private BigDecimal performanceSalary;

    /** 固定补贴合计 */
    @DecimalMin(value = "0", message = "固定补贴不得为负数")
    private BigDecimal allowance;

    /** 生效日期，缺省当天 */
    private LocalDate effectiveDate;
}
