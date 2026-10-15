package com.enterprise.hr.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 评分数值入参（自评 {@code PUT /performance-records/{id}/self-score}
 * 与主管评分 {@code PUT /performance-records/{id}/leader-score} 共用）。
 *
 * <p>请求体驼峰命名，字段名与前端 {@code api/performance.js} 发送的 {@code { score }} 一致。
 * 0~100 的区间校验在此声明，Service 再做一次兜底（防止被直接内部调用绕过 Bean Validation）。
 *
 * @author Enterprise HR Team
 */
@Data
public class ScoreReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评分（0-100） */
    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "0", message = "评分需在 0-100 之间")
    @DecimalMax(value = "100", message = "评分需在 0-100 之间")
    private BigDecimal score;
}
