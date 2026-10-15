package com.enterprise.hr.training.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 培训计划新增结果响应。
 *
 * <p>契约：{@code { success, id, planCode }}；{@code planCode} 为前端新增后回显的驼峰字段。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingPlanCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新计划主键 */
    private Long id;

    /** 计划编号，如 TP20260001 */
    @JsonProperty("planCode")
    private String planCode;

    public static TrainingPlanCreateVO ok(Long id, String planCode) {
        return new TrainingPlanCreateVO(Boolean.TRUE, id, planCode);
    }
}
