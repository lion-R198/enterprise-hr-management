package com.enterprise.hr.notice.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.notice.dto.NoticeCreateReq;
import com.enterprise.hr.notice.dto.NoticeQuery;
import com.enterprise.hr.notice.dto.vo.NoticeVO;
import com.enterprise.hr.system.dto.vo.OpResultVO;

/**
 * 通知公告服务（F-01-03 管理端）。
 *
 * <p>公告有明确的状态机，不允许跳跃：
 * <pre>
 *   草稿(1) --发布--> 已发布(2) --下架--> 已下架(3) --重新发布--> 已发布(2)
 * </pre>
 * 「已发布」不能再次发布（避免发布时间被反复刷新，导致"公告被顶到最新"的骚扰效果），
 * 「草稿」也不能下架（从未发布谈不上下架）。
 *
 * @author Enterprise HR Team
 */
public interface NoticeService {

    /** 公告分页（管理端，含草稿与已下架） */
    PageResult<NoticeVO> page(NoticeQuery query);

    /** 新建公告（默认落为草稿；{@code publishNow=true} 时直接发布） */
    OpResultVO create(NoticeCreateReq req);

    /** 发布公告 */
    OpResultVO publish(Long id);

    /** 下架公告 */
    OpResultVO offline(Long id);
}
