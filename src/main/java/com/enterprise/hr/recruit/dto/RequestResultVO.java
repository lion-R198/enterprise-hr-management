package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 招聘需求新增结果响应。
 *
 * <p>契约：{@code { success, id, requestNo }}。前端 {@code recruit/request.vue} 新增后
 * 立即回显单号（{@code r.requestNo}），故该字段用 {@link JsonProperty} 保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequestResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新需求单主键 */
    private Long id;

    /** 需求单号，如 RC202600001 */
    @JsonProperty("requestNo")
    private String requestNo;

    public static RequestResultVO ok(Long id, String requestNo) {
        return new RequestResultVO(Boolean.TRUE, id, requestNo);
    }
}
