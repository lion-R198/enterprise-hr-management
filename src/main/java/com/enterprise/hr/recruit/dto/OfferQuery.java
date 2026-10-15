package com.enterprise.hr.recruit.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Offer 分页查询入参（F-06-05，{@code GET /offers/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OfferQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 状态：1 待发送 / 2 已发送 / 3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职；空则不筛选 */
    private Integer status;
}
