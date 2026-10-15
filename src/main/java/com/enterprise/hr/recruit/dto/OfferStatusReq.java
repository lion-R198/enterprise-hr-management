package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Offer 状态变更入参（{@code PUT /offers/{id}/status}）。
 *
 * <p>仅允许 3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职；
 * 1 待发送 → 2 已发送 必须走 {@code PUT /offers/{id}/send}。
 *
 * @author Enterprise HR Team
 */
@Data
public class OfferStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职 */
    @NotNull(message = "Offer 状态不能为空")
    private Integer status;
}
