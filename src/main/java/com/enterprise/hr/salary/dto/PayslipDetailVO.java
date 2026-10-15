package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 薪资条明细聚合出参：{@code {record, items}}。
 *
 * <p>契约见 {@code GET /api/salary/payslips/{recordId}/items}：
 * 前端读取 {@code detail.record.salary_year}、{@code detail.record.net_pay}
 * 与 {@code detail.items[].item_name / item_amount / item_type}。
 * 两个字段名均为单词，蛇形策略不会改写；内层对象各自按蛇形序列化。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PayslipDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 薪资记录头（含员工/部门名称） */
    private SalaryRecordVO record;

    /** 11 条薪资明细项，按 sort_no 升序 */
    private List<SalaryItemVO> items;
}
