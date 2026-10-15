package com.enterprise.hr.performance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用操作结果回执 {@code {success}}。
 *
 * <p>用于只关心成败、无需回传业务数据的写操作（方案发布、结果确认）；
 * 前端对这些接口只做 {@code await} 后弹成功提示，不消费额外字段。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SimpleResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;
}
