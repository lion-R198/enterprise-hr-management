package com.enterprise.hr.recruit.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.recruit.dto.OfferQuery;
import com.enterprise.hr.recruit.dto.OfferStatusReq;
import com.enterprise.hr.recruit.dto.OfferVO;
import com.enterprise.hr.recruit.dto.SimpleResultVO;
import com.enterprise.hr.recruit.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offer（录用通知）接口（F-06-05）。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/offers")
@RequiredArgsConstructor
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})
public class OfferController {

    private final OfferService offerService;

    /** Offer 分页（按 Offer 编号倒序） */
    @GetMapping("/page")
    public R<PageResult<OfferVO>> page(@Valid OfferQuery query) {
        return R.ok(offerService.pageOffers(query));
    }

    /** 发送 Offer（仅"待发送"状态可发） */
    @PutMapping("/{id}/send")
    @OperLog(module = "招聘管理", type = OperType.UPDATE, action = "发送 Offer")
    public R<SimpleResultVO> send(@PathVariable("id") Long id) {
        offerService.send(id);
        return R.ok(SimpleResultVO.ok());
    }

    /** 变更 Offer 状态（3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职，6 触发 BR-06-03 入职推送） */
    @PutMapping("/{id}/status")
    @OperLog(module = "招聘管理", type = OperType.UPDATE, action = "变更 Offer 状态")
    public R<SimpleResultVO> changeStatus(@PathVariable("id") Long id,
                                          @Valid @RequestBody OfferStatusReq req) {
        offerService.changeStatus(id, req);
        return R.ok(SimpleResultVO.ok());
    }
}
