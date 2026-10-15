package com.enterprise.hr.attendance.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 加班单分页查询入参（F-04-03，{@code GET /overtime-requests/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OvertimeRequestQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 / 4 已撤销；空则不筛选 */
    private Integer status;

    /** 主管是否查看全部（true 时不受本部门限制，仅对主管角色生效） */
    private Boolean all;

    /** 是否仅看本人 */
    private Boolean mine;
}
