package com.enterprise.hr.performance.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.performance.dto.PerformancePlanCreateReq;
import com.enterprise.hr.performance.dto.PerformancePlanCreateVO;
import com.enterprise.hr.performance.dto.PerformancePlanQuery;
import com.enterprise.hr.performance.dto.PerformancePlanVO;

/**
 * 考核方案（{@code performance_plan}）业务接口。
 *
 * <p>承载 F-08-01 方案列表、方案新增（草稿）与方案发布（BR-08-03）。
 * 方案发布是全模块的"总开关"：发布后才会有 {@code performance_record} 供自评与主管评分。
 *
 * @author Enterprise HR Team
 */
public interface PerformancePlanService {

    /**
     * 方案分页（F-08-01）。
     *
     * <p>出参补齐 {@code creator_name} 与 {@code recordCount}（该方案下已有考核记录数），
     * 关联字段一次性批量查库后在内存拼装，避免逐行查询。
     */
    PageResult<PerformancePlanVO> page(PerformancePlanQuery query);

    /**
     * 新增考核方案（落草稿，{@code status = 1}）。
     *
     * <p>未填字段按契约填默认值：{@code periodType = 2}、{@code scopeType = 1}、
     * {@code gradeRule} 默认区间串、权重 {@code 20 / 70 / 10}；
     * {@code plan_code} 由 {@code BizNoGenerator} 按 {@code PF + 年 + 5 位流水} 生成，
     * {@code creator_id} 取当前登录用户关联员工 ID。
     *
     * @return {@code {success, id, planCode}} 回执
     */
    PerformancePlanCreateVO create(PerformancePlanCreateReq req);

    /**
     * 发布方案：{@code status 1(草稿) → 3(评分中)}（BR-08-03）。
     *
     * <p>BR-08-03：仅草稿状态可发布，其它状态抛 {@code BizException}。
     * 发布即为所有非离职员工生成 {@code performance_record}（{@code status = 1} 待自评），
     * 已存在记录的员工跳过，<b>重复发布不会产生重复记录</b>。
     * 考核主管取员工 {@code manager_id}，为空时兜底员工所在部门的负责人。
     */
    void publish(Long id);
}
