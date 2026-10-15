package com.enterprise.hr.employee.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工异动分页查询入参（{@code GET /employee-changes/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeChangeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 员工 ID */
    private Long employeeId;

    /** 异动类型：1 入职 / 2 转正 / 3 调岗 / 4 调部门 / 5 晋升 / 6 降级 / 7 离职 / 8 返聘 */
    private Integer changeType;
}
