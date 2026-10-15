package com.enterprise.hr.employee.dto.vo;

import com.enterprise.hr.employee.entity.EmployeeContract;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 劳动合同 VO（{@code GET /employee-contracts/page}）。
 *
 * <p>合同表只存员工 ID 与公司名/姓名的签订快照，列表需要"乙方"姓名列
 * （{@code contracts.vue} 绑定 {@code employee_name}），故补该关联字段。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeContractVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 合同编号，如 CT202600001 */
    private String contractNo;

    /** 员工 ID */
    private Long employeeId;

    /** 员工姓名（关联补齐） */
    private String employeeName;

    /** 合同类型：1 固定期限 / 2 无固定期限 / 3 以完成任务为期限 / 4 实习协议 / 5 劳务协议 */
    private Integer contractType;

    /** 甲方（公司名称） */
    private String firstParty;

    /** 乙方（员工姓名，签订时快照） */
    private String secondParty;

    /** 签订日期 */
    private LocalDate signDate;

    /** 合同生效日期 */
    private LocalDate startDate;

    /** 合同到期日期（null = 无固定期限） */
    private LocalDate endDate;

    /** 合同期限（年） */
    private BigDecimal termYears;

    /** 合同约定薪资 */
    private BigDecimal salaryAgreed;

    /** 试用期月数 */
    private Integer probationMonths;

    /** 续签来源合同 ID */
    private Long renewFromId;

    /** 合同状态：1 生效中 / 2 已到期 / 3 已续签 / 4 已解除 / 5 已终止 */
    private Integer status;

    /** 合同扫描件地址 */
    private String attachmentUrl;

    /** 备注 */
    private String remark;

    /** 实体 → VO（employeeName 由 Service 批量补齐） */
    public static EmployeeContractVO from(EmployeeContract contract) {
        if (contract == null) {
            return null;
        }
        EmployeeContractVO vo = new EmployeeContractVO();
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setEmployeeId(contract.getEmployeeId());
        vo.setContractType(contract.getContractType());
        vo.setFirstParty(contract.getFirstParty());
        vo.setSecondParty(contract.getSecondParty());
        vo.setSignDate(contract.getSignDate());
        vo.setStartDate(contract.getStartDate());
        vo.setEndDate(contract.getEndDate());
        vo.setTermYears(contract.getTermYears());
        vo.setSalaryAgreed(contract.getSalaryAgreed());
        vo.setProbationMonths(contract.getProbationMonths());
        vo.setRenewFromId(contract.getRenewFromId());
        vo.setStatus(contract.getStatus());
        vo.setAttachmentUrl(contract.getAttachmentUrl());
        vo.setRemark(contract.getRemark());
        return vo;
    }
}
