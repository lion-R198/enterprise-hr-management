package com.enterprise.hr.salary.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.salary.dto.PayslipDetailVO;
import com.enterprise.hr.salary.dto.SalaryCalculateReq;
import com.enterprise.hr.salary.dto.SalaryPayReq;
import com.enterprise.hr.salary.dto.SalaryRecordQuery;
import com.enterprise.hr.salary.dto.SalaryRecordVO;

/**
 * 月度薪资记录（salary_record / salary_item）业务接口。
 *
 * <p>承载薪资核算（F-05-02）、复核与发放（F-05-03）、我的薪资条（F-05-06）
 * 以及薪资条明细（NF-10 行级隔离）。BR-05-02 / BR-05-03 / BR-05-04 均在此层拦截。
 *
 * @author Enterprise HR Team
 */
public interface SalaryRecordService {

    /**
     * 薪资记录分页（F-05-02）。
     *
     * <p>行级数据范围：主管仅本部门、普通员工仅本人、HR/财务/管理员/决策层全量。
     */
    PageResult<SalaryRecordVO> page(SalaryRecordQuery query);

    /**
     * 发起月度核算。
     *
     * <ul>
     *     <li>BR-05-02：该年月存在未封账的考勤月报时拒绝核算</li>
     *     <li>对每个非离职员工（{@code status != 0}）且当月无记录时生成薪资记录 + 11 条明细</li>
     * </ul>
     *
     * @return 本次新生成的记录条数
     */
    int calculate(SalaryCalculateReq req);

    /**
     * 薪资复核：{@code pay_status 1(已核算) → 2(已复核)}。
     *
     * <ul>
     *     <li>BR-05-03：复核人不得与核算人相同</li>
     *     <li>BR-05-04：已发放并锁定的记录禁止任何修改</li>
     * </ul>
     */
    void review(Long id);

    /**
     * 薪资发放：{@code pay_status 2(已复核) → 3(已发放)}，并落发放日期与批次号。
     *
     * <ul>
     *     <li>BR-05-03：仅"已复核"状态可发放</li>
     *     <li>BR-05-04：发放后置 {@code lock_status = 1} 锁定，后续任何修改均被拒绝</li>
     * </ul>
     */
    void pay(Long id, SalaryPayReq req);

    /** 我的薪资条（F-05-06）：当前登录人关联员工的全部薪资记录，按年月倒序 */
    PageResult<SalaryRecordVO> myPayslips();

    /**
     * 薪资条明细：{@code {record, items}}。
     *
     * <p>NF-10 行级隔离：非本人时仅 HR / FINANCE / ADMIN 可查看，否则 403。
     */
    PayslipDetailVO payslipItems(Long recordId);
}
