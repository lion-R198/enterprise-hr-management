package com.enterprise.hr.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 加班申请入参（{@code POST /overtime-requests}）。
 *
 * <p>起止时间用 {@code String} 承载（{@code HH:mm} 或 {@code HH:mm:ss}）：
 * 全局 Jackson 把 {@code LocalTime} 反序列化器固定为 {@code HH:mm:ss}，
 * 而前端 {@code el-time-select} 默认提交 {@code HH:mm}，直接声明 {@code LocalTime}
 * 会因格式不匹配抛 400。这里在 Service 层做一次宽松解析，兼容两种写法。
 *
 * @author Enterprise HR Team
 */
@Data
public class OvertimeRequestCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 加班日期 */
    @NotNull(message = "加班日期不能为空")
    private LocalDate overtimeDate;

    /** 加班开始时间，如 18:00 */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 加班结束时间，如 21:30 */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 加班时长（小时） */
    @NotNull(message = "加班时长不能为空")
    @DecimalMin(value = "0.5", message = "加班时长不得小于 0.5 小时")
    private BigDecimal hours;

    /** 加班类型：1 工作日 / 2 休息日 / 3 法定节假日，默认 1 */
    private Integer overtimeType;

    /** 补偿方式：1 调休 / 2 计薪，默认 1 */
    private Integer compensateType;

    /** 加班事由 */
    private String reason;
}
