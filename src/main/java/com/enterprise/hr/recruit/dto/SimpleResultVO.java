package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 招聘模块通用"操作成功"响应（审批 / 阶段流转 / 发送 Offer / 变更 Offer 状态）。
 *
 * <p>契约：{@code { success: true }}，与前端 mock 一致。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SimpleResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    public static SimpleResultVO ok() {
        return new SimpleResultVO(Boolean.TRUE);
    }
}
