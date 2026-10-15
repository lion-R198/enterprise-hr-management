package com.enterprise.hr.notice.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告分页查询条件。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 状态：1 草稿 / 2 已发布 / 3 已下架；为空表示全部 */
    private Integer status;

    /** 公告类型：1 公司制度 / 2 放假通知 / 3 人事公告 / 4 活动通知 / 5 其他 */
    private Integer noticeType;

    /** 标题关键字，模糊匹配 */
    private String keyword;
}
