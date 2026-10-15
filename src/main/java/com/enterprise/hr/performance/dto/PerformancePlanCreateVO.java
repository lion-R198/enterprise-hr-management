package com.enterprise.hr.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 新增考核方案的结果回执 {@code {success, id, planCode}}。
 *
 * <p>{@code planCode} 是前端新增成功后回显的方案编号（见
 * {@code docs/backend-contract-brief.md} §3「已知需要保持驼峰的字段」），
 * 故用 {@link JsonProperty} 显式覆盖蛇形策略。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformancePlanCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 新增方案主键 */
    private Long id;

    /** 方案编号（保持驼峰，前端回显用） */
    @JsonProperty("planCode")
    private String planCode;
}
