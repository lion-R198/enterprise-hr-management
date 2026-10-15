package com.enterprise.hr.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出差申请入参（{@code POST /business-trips}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class BusinessTripCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 出差目的地 */
    @NotBlank(message = "出差目的地不能为空")
    private String destination;

    /** 出差事由 */
    private String purpose;

    /** 出差开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 出差结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 出差天数（支持 0.5 天） */
    @NotNull(message = "出差天数不能为空")
    @DecimalMin(value = "0.5", message = "出差天数不得小于 0.5 天")
    private BigDecimal days;

    /** 交通方式：1 飞机 / 2 高铁 / 3 汽车 / 4 自驾 / 5 其他 */
    private Integer transport;

    /** 预算金额（元） */
    private BigDecimal budgetAmount;
}
