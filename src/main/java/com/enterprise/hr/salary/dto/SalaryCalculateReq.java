package com.enterprise.hr.salary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 发起月度薪资核算请求体：{@code {year, month}}。
 *
 * <p>核算前由 Service 校验 BR-05-02（该年月考勤月报必须全部封账）。
 *
 * @author Enterprise HR Team
 */
@Data
public class SalaryCalculateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 薪资年份 */
    @NotNull(message = "核算年份不能为空")
    @Min(value = 2000, message = "核算年份不合理")
    @Max(value = 2100, message = "核算年份不合理")
    private Integer year;

    /** 薪资月份：1-12 */
    @NotNull(message = "核算月份不能为空")
    @Min(value = 1, message = "核算月份取值为 1-12")
    @Max(value = 12, message = "核算月份取值为 1-12")
    private Integer month;
}
