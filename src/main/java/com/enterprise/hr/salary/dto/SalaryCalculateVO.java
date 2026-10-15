package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 月度薪资核算结果回执 {@code {success, created}}。
 *
 * <p>{@code created} 是前端核算成功后提示"生成 N 条薪资记录"的唯一来源
 * （{@code salary/record.vue} 读 {@code res.created}），必须保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryCalculateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 本次新生成的薪资记录条数（已存在当月经录的员工不计入） */
    @JsonProperty("created")
    private int created;
}
