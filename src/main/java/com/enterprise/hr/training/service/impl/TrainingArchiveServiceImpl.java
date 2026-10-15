package com.enterprise.hr.training.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.training.dto.MyTrainingVO;
import com.enterprise.hr.training.dto.TrainingEnrollVO;
import com.enterprise.hr.training.dto.TrainingRecordVO;
import com.enterprise.hr.training.entity.TrainingEnroll;
import com.enterprise.hr.training.entity.TrainingPlan;
import com.enterprise.hr.training.entity.TrainingRecord;
import com.enterprise.hr.training.mapper.TrainingEnrollMapper;
import com.enterprise.hr.training.mapper.TrainingPlanMapper;
import com.enterprise.hr.training.mapper.TrainingRecordMapper;
import com.enterprise.hr.training.service.TrainingArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 我的培训档案业务实现（F-07-06）。
 *
 * <p>只看本人数据：以当前登录账号关联的员工 ID 为准，不接受前端传入员工 ID，
 * 从接口层面杜绝越权查看他人培训档案（NF-10）。
 *
 * <p>响应体为 {@code {records, total, enrolls, totalHours}}，其中 {@code enrolls} /
 * {@code totalHours} 必须保持驼峰（前端 {@code training/my.vue} 直接读取）。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class TrainingArchiveServiceImpl implements TrainingArchiveService {

    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingEnrollMapper trainingEnrollMapper;
    private final TrainingPlanMapper trainingPlanMapper;

    @Override
    public MyTrainingVO myTraining() {
        LoginUser user = UserContext.require();
        Long employeeId = user.getEmployeeId();
        // 契约统一：「我的 xxx」读接口在账号未绑定员工档案时返回空结构，而不是报错。
        // 与 /leave-balances/my、/salary/payslips/my 保持一致：无关联员工 ⇒ 数据为空，
        // 这是正常状态；前端按空表格渲染即可，无需处理错误分支。
        if (employeeId == null) {
            MyTrainingVO empty = new MyTrainingVO();
            empty.setRecords(List.of());
            empty.setTotal(0L);
            empty.setEnrolls(List.of());
            empty.setTotalHours(BigDecimal.ZERO);
            return empty;
        }

        // 培训记录：按培训日期倒序
        List<TrainingRecord> recordList = trainingRecordMapper.selectList(Wrappers.<TrainingRecord>lambdaQuery()
                .eq(TrainingRecord::getEmployeeId, employeeId)
                .orderByDesc(TrainingRecord::getTrainDate)
                .orderByDesc(TrainingRecord::getId));
        List<TrainingRecordVO> records = new ArrayList<>(recordList.size());
        BigDecimal totalHours = BigDecimal.ZERO;
        for (TrainingRecord record : recordList) {
            TrainingRecordVO vo = new TrainingRecordVO();
            BeanUtils.copyProperties(record, vo);
            records.add(vo);
            if (record.getHours() != null) {
                totalHours = totalHours.add(record.getHours());
            }
        }

        // 报名记录：按报名时间倒序，批量补计划名称/状态/起止日期
        List<TrainingEnroll> enrollList = trainingEnrollMapper.selectList(Wrappers.<TrainingEnroll>lambdaQuery()
                .eq(TrainingEnroll::getEmployeeId, employeeId)
                .orderByDesc(TrainingEnroll::getEnrollTime)
                .orderByDesc(TrainingEnroll::getId));
        Map<Long, TrainingPlan> plans = loadPlans(enrollList);
        List<TrainingEnrollVO> enrolls = new ArrayList<>(enrollList.size());
        for (TrainingEnroll enroll : enrollList) {
            TrainingEnrollVO vo = new TrainingEnrollVO();
            BeanUtils.copyProperties(enroll, vo);
            TrainingPlan plan = plans.get(enroll.getPlanId());
            if (plan != null) {
                vo.setPlanName(plan.getPlanName());
                vo.setStatus(plan.getStatus());
                vo.setStartDate(plan.getStartDate());
                vo.setEndDate(plan.getEndDate());
            }
            enrolls.add(vo);
        }

        MyTrainingVO result = new MyTrainingVO();
        result.setRecords(records);
        result.setTotal((long) records.size());
        result.setEnrolls(enrolls);
        result.setTotalHours(totalHours);
        return result;
    }

    /** 一次 IN 查询取回全部关联计划，避免逐条报名查计划 */
    private Map<Long, TrainingPlan> loadPlans(List<TrainingEnroll> enrolls) {
        List<Long> planIds = new ArrayList<>(enrolls.size());
        for (TrainingEnroll enroll : enrolls) {
            if (enroll.getPlanId() != null) {
                planIds.add(enroll.getPlanId());
            }
        }
        if (planIds.isEmpty()) {
            return new HashMap<>(0);
        }
        List<TrainingPlan> list = trainingPlanMapper.selectList(
                Wrappers.<TrainingPlan>lambdaQuery().in(TrainingPlan::getId, planIds));
        Map<Long, TrainingPlan> map = new HashMap<>(list.size() * 2);
        for (TrainingPlan plan : list) {
            map.put(plan.getId(), plan);
        }
        return map;
    }
}
