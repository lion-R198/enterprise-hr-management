package com.enterprise.hr.employee.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工分页查询入参（{@code GET /employees/page}）。
 *
 * <p>查询参数一律驼峰：{@code ?current=1&size=10&keyword=张&deptId=2&positionId=5&status=1}。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 关键字：匹配姓名 / 工号 / 手机号（任一命中即可） */
    private String keyword;

    /** 所属部门 ID */
    private Long deptId;

    /** 所属岗位 ID */
    private Long positionId;

    /** 在职状态：0 离职 / 1 在职 / 2 试用 / 3 待入职 */
    private Integer status;
}
