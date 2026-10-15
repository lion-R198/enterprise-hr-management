package com.enterprise.hr.recruit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.recruit.dto.OfferQuery;
import com.enterprise.hr.recruit.dto.OfferStatusReq;
import com.enterprise.hr.recruit.dto.OfferVO;
import com.enterprise.hr.recruit.entity.Candidate;
import com.enterprise.hr.recruit.entity.OfferRecord;
import com.enterprise.hr.recruit.entity.RecruitRequest;
import com.enterprise.hr.recruit.mapper.CandidateMapper;
import com.enterprise.hr.recruit.mapper.OfferRecordMapper;
import com.enterprise.hr.recruit.mapper.RecruitRequestMapper;
import com.enterprise.hr.recruit.service.OfferService;
import com.enterprise.hr.recruit.service.support.RecruitSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Offer 业务实现（F-06-05）。
 *
 * <p>状态机：1 待发送 →（发送）→ 2 已发送 → 3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职。
 * 其中 3/4/5/6 由本次接口直接指定；6 会触发 <b>BR-06-03 一键推送入职</b>：
 * 自动生成待入职员工档案、回填候选人阶段与招聘需求已录用人数。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class OfferServiceImpl extends ServiceImpl<OfferRecordMapper, OfferRecord> implements OfferService {

    /** 需求状态：已完成 */
    private static final int REQUEST_FINISHED = 2;
    /** 用工性质：3 实习（BR-06-03 约定的"待入职员工"标记值） */
    private static final int EMPLOYEE_TYPE_PENDING = 3;
    /** 性别：未知 */
    private static final String GENDER_UNKNOWN = "U";

    private final CandidateMapper candidateMapper;
    private final RecruitRequestMapper recruitRequestMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionMapper positionMapper;
    private final RecruitSupport recruitSupport;

    @Override
    public PageResult<OfferVO> pageOffers(OfferQuery query) {
        LambdaQueryWrapper<OfferRecord> wrapper = Wrappers.lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(OfferRecord::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(OfferRecord::getOfferNo);

        Page<OfferRecord> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<OfferVO> records = new ArrayList<>(page.getRecords().size());
        for (OfferRecord offer : page.getRecords()) {
            OfferVO vo = new OfferVO();
            BeanUtils.copyProperties(offer, vo);
            records.add(vo);
        }
        recruitSupport.fillOfferVos(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long id) {
        OfferRecord offer = baseMapper.selectById(id);
        if (offer == null) {
            throw BizException.notFound("Offer 不存在");
        }
        if (!Objects.equals(offer.getStatus(), BizConst.OFFER_DRAFT)) {
            throw BizException.badRequest("仅\"待发送\"状态可发送");
        }
        baseMapper.update(null, Wrappers.<OfferRecord>lambdaUpdate()
                .set(OfferRecord::getStatus, BizConst.OFFER_SENT)
                .set(OfferRecord::getSendTime, LocalDateTime.now())
                .eq(OfferRecord::getId, offer.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, OfferStatusReq req) {
        OfferRecord offer = baseMapper.selectById(id);
        if (offer == null) {
            throw BizException.notFound("Offer 不存在");
        }
        int to = req.getStatus();
        if (to != BizConst.OFFER_ACCEPTED && to != BizConst.OFFER_REJECTED
                && to != BizConst.OFFER_EXPIRED && to != BizConst.OFFER_ENTRY) {
            throw BizException.badRequest("无效的 Offer 状态");
        }

        if (to == BizConst.OFFER_ENTRY) {
            // BR-06-03 一键推送入职（防止重复点击造成重复建档）
            if (Objects.equals(offer.getStatus(), BizConst.OFFER_ENTRY)) {
                throw BizException.badRequest("该 Offer 已办理入职，请勿重复操作");
            }
            pushToEmployee(offer);
        }

        LambdaUpdateWrapper<OfferRecord> update = Wrappers.<OfferRecord>lambdaUpdate()
                .set(OfferRecord::getStatus, to)
                .eq(OfferRecord::getId, offer.getId());
        if (to == BizConst.OFFER_ACCEPTED) {
            update.set(OfferRecord::getConfirmTime, LocalDateTime.now());
        }
        baseMapper.update(null, update);
    }

    /**
     * BR-06-03 一键推送入职。
     *
     * <p>三件事在同一事务内完成：
     * <ol>
     *     <li>向 {@code employee} 插入待入职档案（工号顺延，姓名/性别/学历取自候选人，
     *         部门/岗位/职级/薪资取自 Offer）</li>
     *     <li>候选人阶段置"已录用"</li>
     *     <li>招聘需求已录用人数 +1，达到需求人数时需求置"已完成"</li>
     * </ol>
     */
    private void pushToEmployee(OfferRecord offer) {
        Candidate candidate = candidateMapper.selectById(offer.getCandidateId());
        if (candidate == null) {
            throw BizException.notFound("候选人不存在");
        }

        Employee employee = new Employee();
        employee.setEmpNo(BizNoGenerator.empNo(nextEmpNoSeq()));
        employee.setName(candidate.getName());
        employee.setGender(candidate.getGender() == null ? GENDER_UNKNOWN : candidate.getGender());
        employee.setEducation(candidate.getEducation());
        employee.setDeptId(offer.getDeptId());
        employee.setPositionId(offer.getPositionId());
        employee.setPosition(positionNameOf(offer.getPositionId()));
        employee.setJobLevel(offer.getJobLevel());
        employee.setSalary(offer.getOfferSalary());
        // 待入职：用工性质按约定写入 3，状态为"待入职"，入职日期缺省取今天
        employee.setEmployeeType(EMPLOYEE_TYPE_PENDING);
        employee.setHireDate(offer.getEntryDate() == null ? LocalDate.now() : offer.getEntryDate());
        employee.setStatus(BizConst.EMPLOYEE_PENDING);
        employeeMapper.insert(employee);

        candidateMapper.update(null, Wrappers.<Candidate>lambdaUpdate()
                .set(Candidate::getStage, BizConst.CANDIDATE_HIRED)
                .eq(Candidate::getId, candidate.getId()));

        RecruitRequest request = recruitRequestMapper.selectById(offer.getRecruitRequestId());
        if (request != null) {
            int hired = (request.getHiredCount() == null ? 0 : request.getHiredCount()) + 1;
            int need = request.getRecruitCount() == null ? 0 : request.getRecruitCount();
            LambdaUpdateWrapper<RecruitRequest> requestUpdate = Wrappers.<RecruitRequest>lambdaUpdate()
                    .set(RecruitRequest::getHiredCount, hired)
                    .eq(RecruitRequest::getId, request.getId());
            if (hired >= need) {
                requestUpdate.set(RecruitRequest::getStatus, REQUEST_FINISHED);
            }
            recruitRequestMapper.update(null, requestUpdate);
        }
    }

    /** 取"现有最大工号序号 + 1"，保证新工号唯一且单调 */
    private long nextEmpNoSeq() {
        List<Employee> list = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .select(Employee::getEmpNo));
        long max = 0L;
        for (Employee employee : list) {
            long seq = BizNoGenerator.parseEmpNoSeq(employee.getEmpNo());
            if (seq > max) {
                max = seq;
            }
        }
        return max + 1;
    }

    /** 岗位名称快照（岗位不存在时留空，不影响入职建档） */
    private String positionNameOf(Long positionId) {
        if (positionId == null) {
            return null;
        }
        Position position = positionMapper.selectById(positionId);
        return position == null ? null : position.getPositionName();
    }
}
