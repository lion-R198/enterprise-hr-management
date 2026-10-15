package com.enterprise.hr.training.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 我的培训档案响应（F-07-06 员工自助查看，{@code GET /training/my}）。
 *
 * <p>契约：{@code { records, total, enrolls, totalHours }}。
 * 前端 {@code training/my.vue} 直接读 {@code data.enrolls} 与 {@code data.totalHours}，
 * 这两个字段用 {@link JsonProperty} 保持驼峰，不可改为蛇形。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MyTrainingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 培训档案明细（本人 training_record，按培训日期倒序） */
    private List<TrainingRecordVO> records;

    /** 档案条数 */
    private Long total;

    /** 本人报名记录（含计划名称与计划状态） */
    @JsonProperty("enrolls")
    private List<TrainingEnrollVO> enrolls;

    /** 累计培训学时 */
    @JsonProperty("totalHours")
    private BigDecimal totalHours;
}
