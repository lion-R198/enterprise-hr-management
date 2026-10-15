package com.enterprise.hr.recruit.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.recruit.dto.CandidateVO;
import com.enterprise.hr.recruit.dto.InterviewRecordVO;
import com.enterprise.hr.recruit.dto.OfferVO;
import com.enterprise.hr.recruit.dto.RecruitRequestVO;
import com.enterprise.hr.recruit.entity.Candidate;
import com.enterprise.hr.recruit.entity.RecruitRequest;
import com.enterprise.hr.recruit.mapper.CandidateMapper;
import com.enterprise.hr.recruit.mapper.RecruitRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 招聘模块展示信息查询支持。
 *
 * <p>需求/候选人/面试/Offer 四类列表都要补"部门名、员工名、岗位名、需求单号"，
 * 且同一列表内的关联 ID 高度重复。统一收敛到这里，<b>全部为批量 IN 查询 + 内存映射</b>，
 * 杜绝在循环里逐条查库（N+1）。
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class RecruitSupport {

    /** 招聘需求单号前缀（RC = ReCruit） */
    private static final String REQUEST_NO_PREFIX = "RC";

    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;
    private final PositionMapper positionMapper;
    private final RecruitRequestMapper recruitRequestMapper;
    private final CandidateMapper candidateMapper;

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

    /** 批量查部门名称：{@code deptId → name} */
    public Map<Long, String> loadDeptNames(Collection<Long> deptIds) {
        Set<Long> ids = distinct(deptIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Dept> list = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .select(Dept::getId, Dept::getName)
                .in(Dept::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Dept dept : list) {
            map.put(dept.getId(), dept.getName());
        }
        return map;
    }

    /** 批量查岗位名称：{@code positionId → positionName} */
    public Map<Long, String> loadPositionNames(Collection<Long> positionIds) {
        Set<Long> ids = distinct(positionIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Position> list = positionMapper.selectList(Wrappers.<Position>lambdaQuery()
                .select(Position::getId, Position::getPositionName)
                .in(Position::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Position position : list) {
            map.put(position.getId(), position.getPositionName());
        }
        return map;
    }

    /** 批量查招聘需求：{@code requestId → RecruitRequest} */
    public Map<Long, RecruitRequest> loadRecruitRequests(Collection<Long> requestIds) {
        Set<Long> ids = distinct(requestIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RecruitRequest> list = recruitRequestMapper.selectList(
                Wrappers.<RecruitRequest>lambdaQuery().in(RecruitRequest::getId, ids));
        Map<Long, RecruitRequest> map = new HashMap<>(list.size() * 2);
        for (RecruitRequest request : list) {
            map.put(request.getId(), request);
        }
        return map;
    }

    /** 批量查候选人：{@code candidateId → Candidate} */
    public Map<Long, Candidate> loadCandidates(Collection<Long> candidateIds) {
        Set<Long> ids = distinct(candidateIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Candidate> list = candidateMapper.selectList(
                Wrappers.<Candidate>lambdaQuery().in(Candidate::getId, ids));
        Map<Long, Candidate> map = new HashMap<>(list.size() * 2);
        for (Candidate candidate : list) {
            map.put(candidate.getId(), candidate);
        }
        return map;
    }

    /** 补全招聘需求列表的 {@code dept_name} / {@code applicant_name} */
    public void fillRecruitRequestVos(List<RecruitRequestVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> deptIds = new ArrayList<>(vos.size());
        List<Long> applicantIds = new ArrayList<>(vos.size());
        for (RecruitRequestVO vo : vos) {
            deptIds.add(vo.getRequestDeptId());
            applicantIds.add(vo.getApplicantId());
        }
        Map<Long, String> deptNames = loadDeptNames(deptIds);
        Map<Long, String> applicantNames = loadEmployeeNames(applicantIds);
        for (RecruitRequestVO vo : vos) {
            vo.setDeptName(deptNames.get(vo.getRequestDeptId()));
            vo.setApplicantName(vo.getApplicantId() == null ? null : applicantNames.get(vo.getApplicantId()));
        }
    }

    /** 补全候选人列表的 {@code request_no} / {@code position_name}（取自关联需求快照） */
    public void fillCandidateVos(List<CandidateVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> requestIds = new ArrayList<>(vos.size());
        for (CandidateVO vo : vos) {
            requestIds.add(vo.getRecruitRequestId());
        }
        Map<Long, RecruitRequest> requests = loadRecruitRequests(requestIds);
        for (CandidateVO vo : vos) {
            RecruitRequest request = requests.get(vo.getRecruitRequestId());
            if (request != null) {
                vo.setRequestNo(request.getRequestNo());
                vo.setPositionName(request.getPositionName());
            }
        }
    }

    /** 补全面试记录的 {@code candidate_name} / {@code interviewer_name} */
    public void fillInterviewVos(List<InterviewRecordVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> candidateIds = new ArrayList<>(vos.size());
        List<Long> interviewerIds = new ArrayList<>(vos.size());
        for (InterviewRecordVO vo : vos) {
            candidateIds.add(vo.getCandidateId());
            interviewerIds.add(vo.getInterviewerId());
        }
        Map<Long, Candidate> candidates = loadCandidates(candidateIds);
        Map<Long, String> interviewerNames = loadEmployeeNames(interviewerIds);
        for (InterviewRecordVO vo : vos) {
            Candidate candidate = candidates.get(vo.getCandidateId());
            vo.setCandidateName(candidate == null ? null : candidate.getName());
            vo.setInterviewerName(vo.getInterviewerId() == null ? null : interviewerNames.get(vo.getInterviewerId()));
        }
    }

    /** 补全 Offer 列表的 {@code candidate_name} / {@code dept_name} / {@code position_name} */
    public void fillOfferVos(List<OfferVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> candidateIds = new ArrayList<>(vos.size());
        List<Long> deptIds = new ArrayList<>(vos.size());
        List<Long> requestIds = new ArrayList<>(vos.size());
        List<Long> positionIds = new ArrayList<>(vos.size());
        for (OfferVO vo : vos) {
            candidateIds.add(vo.getCandidateId());
            deptIds.add(vo.getDeptId());
            requestIds.add(vo.getRecruitRequestId());
            positionIds.add(vo.getPositionId());
        }
        Map<Long, Candidate> candidates = loadCandidates(candidateIds);
        Map<Long, String> deptNames = loadDeptNames(deptIds);
        Map<Long, String> positionNames = loadPositionNames(positionIds);
        Map<Long, RecruitRequest> requests = loadRecruitRequests(requestIds);
        for (OfferVO vo : vos) {
            Candidate candidate = candidates.get(vo.getCandidateId());
            vo.setCandidateName(candidate == null ? null : candidate.getName());
            vo.setDeptName(deptNames.get(vo.getDeptId()));
            RecruitRequest request = requests.get(vo.getRecruitRequestId());
            // 岗位名优先取需求快照（与前端 mock 口径一致），缺省回退岗位主数据
            vo.setPositionName(request == null ? positionNames.get(vo.getPositionId()) : request.getPositionName());
        }
    }

    /** 生成招聘需求单号：{@code RC + 年份 + 5 位流水} */
    public String nextRequestNo() {
        Long count = recruitRequestMapper.selectCount(Wrappers.<RecruitRequest>lambdaQuery()
                .likeRight(RecruitRequest::getRequestNo, BizNoGenerator.yearPrefix(REQUEST_NO_PREFIX)));
        return BizNoGenerator.next(REQUEST_NO_PREFIX, (count == null ? 0L : count) + 1);
    }

    /** 去重去空，保持插入顺序 */
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
