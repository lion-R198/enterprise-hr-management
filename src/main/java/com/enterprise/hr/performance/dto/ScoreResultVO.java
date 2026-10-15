package com.enterprise.hr.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 主管评分结果回执 {@code {success, finalScore, grade}}。
 *
 * <p>前端 {@code performance/record.vue} 直接读取 {@code r.finalScore} / {@code r.grade}
 * 拼提示语「评分完成：总分 xx，等级 xx」，故两者均用 {@link JsonProperty} 保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ScoreResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 最终得分（保持驼峰，前端直读） */
    @JsonProperty("finalScore")
    private BigDecimal finalScore;

    /** 绩效等级 S/A/B/C/D（保持驼峰，前端直读） */
    @JsonProperty("grade")
    private String grade;
}
