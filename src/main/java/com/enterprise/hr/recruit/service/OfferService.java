package com.enterprise.hr.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.recruit.dto.OfferQuery;
import com.enterprise.hr.recruit.dto.OfferStatusReq;
import com.enterprise.hr.recruit.dto.OfferVO;
import com.enterprise.hr.recruit.entity.OfferRecord;

/**
 * Offer 业务接口（F-06-05）。
 *
 * @author Enterprise HR Team
 */
public interface OfferService extends IService<OfferRecord> {

    /** Offer 分页（按 Offer 编号倒序） */
    PageResult<OfferVO> pageOffers(OfferQuery query);

    /** 发送 Offer（仅"待发送"可发） */
    void send(Long id);

    /** 变更 Offer 状态（3/4/5/6；6 触发 BR-06-03 一键推送入职） */
    void changeStatus(Long id, OfferStatusReq req);
}
