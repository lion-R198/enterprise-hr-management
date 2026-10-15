package com.enterprise.hr.salary.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 月度薪资记录分页查询入参（前端 {@code salary/record.vue}）。
 *
 * <p>查询参数一律驼峰：{@code ?current=1&size=20&year=2026&month=9&payStatus=1&deptId=2&keyword=张}。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SalaryRecordQuery extends PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 薪资年份，如 2026 */
    private Integer year;

    /** 薪资月份：1-12 */
    private Integer month;

    /** 发放状态：0 待核算 / 1 已核算 / 2 已复核 / 3 已发放 / 4 发放失败 */
    private Integer payStatus;

    /** 所属部门 ID */
    private Long deptId;

    /** 关键字：姓名或工号模糊匹配 */
    private String keyword;
}
