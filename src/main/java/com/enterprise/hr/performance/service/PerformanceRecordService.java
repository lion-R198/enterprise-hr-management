package com.enterprise.hr.performance.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.performance.dto.ConfirmReq;
import com.enterprise.hr.performance.dto.PerformanceRecordQuery;
import com.enterprise.hr.performance.dto.PerformanceRecordVO;
import com.enterprise.hr.performance.dto.ScoreReq;
import com.enterprise.hr.performance.dto.ScoreResultVO;
import com.enterprise.hr.performance.dto.SelfScoreResultVO;

/**
 * 绩效考核记录（{@code performance_record}）业务接口。
 *
 * <p>承载 F-08-03 的完整流程机：
 * {@code 1 待自评 → 2 待主管评 → 4 待员工确认 → 5 已完成}，
 * 其中员工申诉（BR-08-01）不推进流程，只把确认状态置为"已申诉"等待复核。
 *
 * @author Enterprise HR Team
 */
public interface PerformanceRecordService {

    /**
     * 考核记录分页（F-08-03）。
     *
     * <p>NF-10 行级数据范围：普通员工仅本人、部门主管仅本部门、
     * HR / ADMIN / EXECUTIVE 全量（由 {@code DataScopeHelper} 判定）。
     * 出参补齐 {@code employee_name} / {@code emp_no} / {@code dept_name}
     * / {@code leader_name} / {@code plan_name}。
     */
    PageResult<PerformanceRecordVO> page(PerformanceRecordQuery query);

    /**
     * 员工提交自评：{@code status 1(待自评) → 2(待主管评)}。
     *
     * <ul>
     *     <li>仅本人可提交（否则 403「仅可提交本人自评」）</li>
     *     <li>非"待自评"状态拒绝</li>
     *     <li>分数必须在 0~100 之间</li>
     * </ul>
     *
     * @return {@code {success, weightNote}}，weightNote 形如「自评权重 20%」
     */
    SelfScoreResultVO selfScore(Long id, ScoreReq req);

    /**
     * 主管评分：{@code status 2(待主管评) → 4(待员工确认)}，并按方案权重算总分与等级。
     *
     * <ul>
     *     <li>仅 MANAGER / HR（及 ADMIN）可评分</li>
     *     <li>非"待主管评"状态拒绝</li>
     *     <li>总分 = 自评 × 自评权重% + 主管评 × 主管权重%（2 位小数、四舍五入）</li>
     *     <li>等级由方案 {@code grade_rule} 解析定档（阈值不写死在代码里）</li>
     * </ul>
     *
     * @return {@code {success, finalScore, grade}}
     */
    ScoreResultVO leaderScore(Long id, ScoreReq req);

    /**
     * 员工确认考核结果（BR-08-01）。
     *
     * <p>BR-08-01 申诉分支：{@code appeal = true} 时置
     * {@code employee_confirm_status = 2} 并记录 {@code appeal_reason}，
     * <b>流程状态不推进</b>（停留在待确认，等待 HR/复核人处理）；
     * 否则置 {@code employee_confirm_status = 1}、{@code status = 5}（已完成）。
     *
     * <p>仅本人可确认；非"待员工确认"状态抛「当前状态不允许确认」。
     */
    void confirm(Long id, ConfirmReq req);
}
