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
 * 员工自评结果回执 {@code {success, weightNote}}。
 *
 * <p>{@code weightNote} 形如「自评权重 20%」，前端自评成功后提示用；
 * 该字段在前端被直接读取，故用 {@link JsonProperty} 保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SelfScoreResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 权重提示，如「自评权重 20%」（保持驼峰，前端直读） */
    @JsonProperty("weightNote")
    private String weightNote;
}
