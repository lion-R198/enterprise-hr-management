package com.enterprise.hr.training.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.training.entity.Course;
import com.enterprise.hr.training.entity.TrainingEnroll;
import com.enterprise.hr.training.entity.TrainingPlan;
import com.enterprise.hr.training.entity.TrainingRecord;
import com.enterprise.hr.training.mapper.CourseMapper;
import com.enterprise.hr.training.mapper.TrainingEnrollMapper;
import com.enterprise.hr.training.mapper.TrainingPlanMapper;
import com.enterprise.hr.training.mapper.TrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 培训模块展示与统计信息查询支持。
 *
 * <p>培训计划列表要补"组织人姓名、已报名人数、关联课程名"三类派生字段，
 * 全部按 {@code plan_id} 维度<b>批量</b>查询后在内存聚合，避免逐行查库。
 *
 * <p><b>培训计划与课程的关联口径</b>：{@code training_plan} 表没有直接的课程外键，
 * 两者只能经由 {@code training_enroll}（报名时落 course_id）或 {@code training_record}
 * （归档时落 course_id）间接关联。本类采用"报名表优先、培训档案兜底"的口径。
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class TrainingSupport {

    /** 培训计划编号前缀（TP = Training Plan） */
    private static final String PLAN_CODE_PREFIX = "TP";
    /** 课程名连接符 */
    private static final String COURSE_NAME_SEPARATOR = "、";

    private final EmployeeMapper employeeMapper;
    private final CourseMapper courseMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingEnrollMapper trainingEnrollMapper;
    private final TrainingRecordMapper trainingRecordMapper;

    /** 按 ID 查员工，查不到抛 404 */
    public Employee requireEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "当前登录账号未关联员工档案");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + employeeId);
        }
        return employee;
    }

    /** 批量查员工姓名：{@code employeeId → name} */
    public Map<Long, String> loadEmployeeNames(Collection<Long> employeeIds) {
        Set<Long> ids = distinct(employeeIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Employee> list = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .select(Employee::getId, Employee::getName)
                .in(Employee::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Employee employee : list) {
            map.put(employee.getId(), employee.getName());
        }
        return map;
    }

    /** 批量统计各计划的已报名人数：{@code planId → count}（无报名的计划不在 map 中） */
    public Map<Long, Integer> loadEnrollCounts(Collection<Long> planIds) {
        Set<Long> ids = distinct(planIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TrainingEnroll> list = trainingEnrollMapper.selectList(Wrappers.<TrainingEnroll>lambdaQuery()
                .select(TrainingEnroll::getPlanId)
                .in(TrainingEnroll::getPlanId, ids));
        Map<Long, Integer> map = new HashMap<>(list.size() * 2);
        for (TrainingEnroll enroll : list) {
            map.merge(enroll.getPlanId(), 1, Integer::sum);
        }
        return map;
    }

    /**
     * 批量取各计划的关联课程名（多个以「、」连接）：{@code planId → "课程A、课程B"}。
     *
     * <p>口径：先取该计划的报名课程（{@code training_enroll}），尚无报名记录时回退培训档案
     * （{@code training_record}），再按课程ID升序去重拼接，保证同一计划的展示顺序稳定。
     */
    public Map<Long, String> loadCourseNames(Collection<Long> planIds) {
        Set<Long> ids = distinct(planIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Set<Long>> planCourses = new LinkedHashMap<>();
        for (Long planId : ids) {
            planCourses.put(planId, new LinkedHashSet<>());
        }
        // 1) 报名表优先
        List<TrainingEnroll> enrolls = trainingEnrollMapper.selectList(Wrappers.<TrainingEnroll>lambdaQuery()
                .select(TrainingEnroll::getPlanId, TrainingEnroll::getCourseId)
                .in(TrainingEnroll::getPlanId, ids));
        for (TrainingEnroll enroll : enrolls) {
            addCourse(planCourses, enroll.getPlanId(), enroll.getCourseId());
        }
        // 2) 仍无课程的计划回退到培训档案
        List<Long> missing = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : planCourses.entrySet()) {
            if (entry.getValue().isEmpty()) {
                missing.add(entry.getKey());
            }
        }
        if (!missing.isEmpty()) {
            List<TrainingRecord> records = trainingRecordMapper.selectList(Wrappers.<TrainingRecord>lambdaQuery()
                    .select(TrainingRecord::getPlanId, TrainingRecord::getCourseId)
                    .in(TrainingRecord::getPlanId, missing)
                    .isNotNull(TrainingRecord::getCourseId));
            for (TrainingRecord record : records) {
                addCourse(planCourses, record.getPlanId(), record.getCourseId());
            }
        }
        // 3) 一次性取课程名后拼装
        Set<Long> courseIds = new LinkedHashSet<>();
        for (Set<Long> values : planCourses.values()) {
            courseIds.addAll(values);
        }
        Map<Long, String> courseNames = loadCourseNameMap(courseIds);
        Map<Long, String> result = new HashMap<>(ids.size() * 2);
        for (Map.Entry<Long, Set<Long>> entry : planCourses.entrySet()) {
            List<Long> ordered = new ArrayList<>(entry.getValue());
            Collections.sort(ordered);
            List<String> names = new ArrayList<>(ordered.size());
            for (Long courseId : ordered) {
                String name = courseNames.get(courseId);
                if (name != null) {
                    names.add(name);
                }
            }
            result.put(entry.getKey(), String.join(COURSE_NAME_SEPARATOR, names));
        }
        return result;
    }

    /** 生成培训计划编号：{@code TP + 年份 + 5 位流水} */
    public String nextPlanCode() {
        Long count = trainingPlanMapper.selectCount(Wrappers.<TrainingPlan>lambdaQuery()
                .likeRight(TrainingPlan::getPlanCode, BizNoGenerator.yearPrefix(PLAN_CODE_PREFIX)));
        return BizNoGenerator.next(PLAN_CODE_PREFIX, (count == null ? 0L : count) + 1);
    }

    /** 按 ID 批量取课程名 */
    private Map<Long, String> loadCourseNameMap(Collection<Long> courseIds) {
        Set<Long> ids = distinct(courseIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Course> list = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
                .select(Course::getId, Course::getCourseName)
                .in(Course::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Course course : list) {
            map.put(course.getId(), course.getCourseName());
        }
        return map;
    }

    private static void addCourse(Map<Long, Set<Long>> planCourses, Long planId, Long courseId) {
        if (planId == null || courseId == null) {
            return;
        }
        Set<Long> courses = planCourses.get(planId);
        if (courses != null) {
            courses.add(courseId);
        }
    }

    private static Set<Long> distinct(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (Objects.nonNull(id)) {
                set.add(id);
            }
        }
        return set;
    }
}
