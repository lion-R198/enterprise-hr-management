package com.enterprise.hr.employee.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 劳动合同分页查询入参（{@code GET /employee-contracts/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeContractQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 员工 ID */
    private Long employeeId;

    /** 合同状态：1 生效中 / 2 已到期 / 3 已续签 / 4 已解除 / 5 已终止 */
    private Integer status;
}
