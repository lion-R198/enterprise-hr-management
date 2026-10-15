package com.enterprise.hr.employee.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 劳动合同签订/续签入参（{@code POST /employee-contracts}）。
 *
 * <p>必填 {@code employeeId / startDate}；合同编号、甲方（公司名）、乙方（员工姓名）
 * 由服务端生成/取快照，不接受前端传入，避免合同主体被篡改。
 *
 * @author Enterprise HR Team
 */
@Data
public class EmployeeContractCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 员工 ID（必填） */
    private Long employeeId;

    /** 合同生效日期（必填） */
    private LocalDate startDate;

    /** 合同类型：1 固定期限 / 2 无固定期限 / 3 以完成任务为期限 / 4 实习协议 / 5 劳务协议，默认 1 */
    private Integer contractType;

    /** 签订日期，不传取当天 */
    private LocalDate signDate;

    /** 合同到期日期（无固定期限可不传） */
    private LocalDate endDate;

    /** 合同期限（年） */
    private BigDecimal termYears;

    /** 合同约定薪资 */
    private BigDecimal salaryAgreed;

    /** 试用期月数，默认 0 */
    private Integer probationMonths;

    /** 备注 */
    private String remark;
}
