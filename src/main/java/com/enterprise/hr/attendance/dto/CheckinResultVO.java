package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上班打卡结果响应（{@code POST /attendance/checkin}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CheckinResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /**
     * 打卡时间。
     *
     * <p>前端 {@code checkin.vue} 读 {@code r.checkinTime}，必须保持驼峰，故显式覆盖蛇形策略。
     */
    @JsonProperty("checkinTime")
    private LocalDateTime checkinTime;
}
