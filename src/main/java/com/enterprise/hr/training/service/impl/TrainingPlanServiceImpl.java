package com.enterprise.hr.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.training.dto.TrainingPlanCreateReq;
import com.enterprise.hr.training.dto.TrainingPlanCreateVO;
import com.enterprise.hr.training.dto.TrainingPlanQuery;
import com.enterprise.hr.training.dto.TrainingPlanVO;
import com.enterprise.hr.training.entity.Course;
import com.enterprise.hr.training.entity.TrainingEnroll;
import com.enterprise.hr.training.entity.TrainingPlan;
import com.enterprise.hr.training.mapper.CourseMapper;
import com.enterprise.hr.training.mapper.TrainingEnrollMapper;
import com.enterprise.hr.training.mapper.TrainingPlanMapper;
import com.enterprise.hr.training.service.TrainingPlanService;
import com.enterprise.hr.training.service.support.TrainingSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 培训计划业务实现（F-07-01 / F-07-02）。
 *
 * <p>报名前置条件：计划必须处于"已发布(2)"或"进行中(3)"；同一员工同一计划不可重复报名；
 * 计划关联课程设置人数上限时，达到上限即拒绝报名（BR-07-01 满员自动关闭）。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl extends ServiceImpl<TrainingPlanMapper, TrainingPlan>
        implements TrainingPlanService {

    /** 培训类型缺省值：专业技能 */
    private static final int TRAIN_TYPE_DEFAULT = 2;
    /** 培训对象范围缺省值：全体 */
    private static final int TARGET_SCOPE_ALL = 1;

    private final TrainingSupport trainingSupport;
    private final TrainingEnrollMapper trainingEnrollMapper;
    private final CourseMapper courseMapper;

    @Override
    public PageResult<TrainingPlanVO> pagePlans(TrainingPlanQuery query) {
        LambdaQueryWrapper<TrainingPlan> wrapper = Wrappers.lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(TrainingPlan::getStatus, query.getStatus());
        }
        if (query.getTrainType() != null) {
            wrapper.eq(TrainingPlan::getTrainType, query.getTrainType());
        }
        wrapper.orderByDesc(TrainingPlan::getStartDate).orderByDesc(TrainingPlan::getId);

        Page<TrainingPlan> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<TrainingPlanVO> records = new ArrayList<>(page.getRecords().size());
        List<Long> planIds = new ArrayList<>(page.getRecords().size());
        List<Long> organizerIds = new ArrayList<>(page.getRecords().size());
        for (TrainingPlan plan : page.getRecords()) {
            TrainingPlanVO vo = new TrainingPlanVO();
            BeanUtils.copyProperties(plan, vo);
            records.add(vo);
            planIds.add(plan.getId());
            organizerIds.add(plan.getOrganizerId());
        }
        // 三类派生字段各自一次批量查询，整页数据零 N+1
        Map<Long, String> organizerNames = trainingSupport.loadEmployeeNames(organizerIds);
        Map<Long, Integer> enrollCounts = trainingSupport.loadEnrollCounts(planIds);
        Map<Long, String> courseNames = trainingSupport.loadCourseNames(planIds);
        for (TrainingPlanVO vo : records) {
            vo.setOrganizerName(vo.getOrganizerId() == null ? null : organizerNames.get(vo.getOrganizerId()));
            vo.setEnrollCount(enrollCounts.getOrDefault(vo.getId(), 0));
            vo.setCourseNames(courseNames.getOrDefault(vo.getId(), ""));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainingPlanCreateVO create(TrainingPlanCreateReq req) {
        LoginUser user = UserContext.require();
        Long organizerId = requireEmployeeId(user);

        TrainingPlan entity = new TrainingPlan();
        entity.setPlanCode(trainingSupport.nextPlanCode());
        entity.setPlanName(req.getPlanName());
        entity.setPlanYear(req.getPlanYear());
        entity.setPlanQuarter(req.getPlanQuarter() == null ? 0 : req.getPlanQuarter());
        entity.setTrainType(req.getTrainType() == null ? TRAIN_TYPE_DEFAULT : req.getTrainType());
        entity.setTargetScope(req.getTargetScope() == null ? TARGET_SCOPE_ALL : req.getTargetScope());
        entity.setBudgetAmount(req.getBudgetAmount() == null ? BigDecimal.ZERO : req.getBudgetAmount());
        entity.setActualAmount(BigDecimal.ZERO);
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setOrganizerId(organizerId);
        entity.setDescription(req.getDescription());
        // 新建计划为草稿，发布后才开放报名
        entity.setStatus(BizConst.TRAINING_DRAFT);
        baseMapper.insert(entity);

        return TrainingPlanCreateVO.ok(entity.getId(), entity.getPlanCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enroll(Long id) {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        TrainingPlan plan = baseMapper.selectById(id);
        if (plan == null) {
            throw BizException.notFound("培训计划不存在");
        }
        Integer status = plan.getStatus();
        if (!Objects.equals(status, BizConst.TRAINING_ENROLLING)
                && !Objects.equals(status, BizConst.TRAINING_ONGOING)) {
            throw BizException.badRequest("该计划未开放报名");
        }

        // 同一员工同一计划仅允许报名一次
        Long duplicated = trainingEnrollMapper.selectCount(Wrappers.<TrainingEnroll>lambdaQuery()
                .eq(TrainingEnroll::getPlanId, plan.getId())
                .eq(TrainingEnroll::getEmployeeId, employeeId));
        if (duplicated != null && duplicated > 0) {
            throw new BizException(ErrorCode.TRAINING_ENROLL_DUPLICATED);
        }

        Long courseId = resolveCourseId(plan.getId());
        long enrolled = countEnrolls(plan.getId());
        Course course = courseMapper.selectById(courseId);
        // BR-07-01 满员自动关闭：课程设置了人数上限(>0)且报名数已达上限时拒绝
        if (course != null && course.getMaxStudents() != null && course.getMaxStudents() > 0
                && enrolled >= course.getMaxStudents()) {
            throw new BizException(ErrorCode.TRAINING_ENROLL_FULL);
        }

        TrainingEnroll entity = new TrainingEnroll();
        entity.setPlanId(plan.getId());
        entity.setCourseId(courseId);
        entity.setEmployeeId(employeeId);
        entity.setEnrollTime(LocalDateTime.now());
        entity.setSignInStatus(0);
        entity.setPassStatus(0);
        trainingEnrollMapper.insert(entity);
    }

    /**
     * 解析报名所用课程：同计划已有报名时沿用同一门课，否则取课程表中首门启用课程。
     *
     * <p>原因：{@code training_enroll.course_id} 非空，而培训计划与课程没有直接外键
     * （关联口径见 {@link TrainingSupport}）。
     */
    private Long resolveCourseId(Long planId) {
        List<TrainingEnroll> exists = trainingEnrollMapper.selectList(Wrappers.<TrainingEnroll>lambdaQuery()
                .eq(TrainingEnroll::getPlanId, planId)
                .orderByAsc(TrainingEnroll::getId)
                .last("LIMIT 1"));
        if (!exists.isEmpty() && exists.get(0).getCourseId() != null) {
            return exists.get(0).getCourseId();
        }
        List<Course> courses = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
                .eq(Course::getStatus, BizConst.STATUS_ENABLED)
                .orderByAsc(Course::getId)
                .last("LIMIT 1"));
        if (courses.isEmpty()) {
            throw BizException.badRequest("暂无可报名课程，请联系培训管理员");
        }
        return courses.get(0).getId();
    }

    /** 计划已报名人数 */
    private long countEnrolls(Long planId) {
        Long count = trainingEnrollMapper.selectCount(Wrappers.<TrainingEnroll>lambdaQuery()
                .eq(TrainingEnroll::getPlanId, planId));
        return count == null ? 0L : count;
    }

    /** 当前登录账号必须关联员工档案，否则无法落库报名人 */
    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案");
        }
        return employeeId;
    }
}
