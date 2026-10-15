package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 新增薪资标准的结果回执 {@code {success, id, standardCode}}。
 *
 * <p>{@code standardCode} 前端用于新增成功后回显编号（见
 * {@code docs/backend-contract-brief.md} §3 "已知需要保持驼峰的字段"），
 * 因此用 {@link JsonProperty} 显式覆盖蛇形策略。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryStandardCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 新增记录主键 */
    private Long id;

    /** 标准编码（保持驼峰，前端回显用） */
    @JsonProperty("standardCode")
    private String standardCode;
}
