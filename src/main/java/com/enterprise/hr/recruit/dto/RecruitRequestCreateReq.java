package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 提报招聘需求入参（{@code POST /recruit-requests}）。
 *
 * <p>请求体统一为驼峰字段，与前端 {@code recruit/request.vue} 的 {@code form} 一致。
 *
 * @author Enterprise HR Team
 */
@Data
public class RecruitRequestCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 需求部门ID */
    @NotNull(message = "需求部门不能为空")
    private Long requestDeptId;

    /** 招聘岗位名称（快照） */
    @NotBlank(message = "招聘岗位不能为空")
    private String positionName;

    /** 招聘人数 */
    @NotNull(message = "招聘人数不能为空")
    @Min(value = 1, message = "招聘人数不得小于 1")
    private Integer recruitCount;

    /** 招聘岗位ID（可为空，仅作关联） */
    private Long positionId;

    /** 职级，如 P5 */
    private String jobLevel;

    /** 薪资区间下限 */
    private BigDecimal salaryMin;

    /** 薪资区间上限 */
    private BigDecimal salaryMax;

    /** 学历要求：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 不限 */
    private Integer educationReq;

    /** 经验要求（年） */
    private Integer experienceYears;

    /** 岗位职责 */
    private String jobDescription;

    /** 任职要求 */
    private String jobRequirement;

    /** 期望到岗日期 */
    private LocalDate hopeArriveDate;

    /** 紧急程度：1 普通 / 2 紧急 / 3 非常紧急；空则按普通处理 */
    private Integer urgency;
}
