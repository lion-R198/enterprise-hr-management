package com.enterprise.hr.employee.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位分页查询入参（{@code GET /positions}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PositionQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 所属部门 ID */
    private Long deptId;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
