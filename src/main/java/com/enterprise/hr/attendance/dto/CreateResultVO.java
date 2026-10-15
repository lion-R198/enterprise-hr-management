package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务单据创建结果响应（请假 / 加班 / 出差新增）。
 *
 * <p>契约：{@code { success, id, requestNo }}。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新单据主键 */
    private Long id;

    /**
     * 申请单号，如 LV202600001。
     *
     * <p>前端新增后需要立即回显单号，契约字段为驼峰 {@code requestNo}，故显式覆盖蛇形策略。
     */
    @JsonProperty("requestNo")
    private String requestNo;
}
