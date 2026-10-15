package com.enterprise.hr.recruit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.recruit.dto.RecruitApprovalReq;
import com.enterprise.hr.recruit.dto.RecruitRequestCreateReq;
import com.enterprise.hr.recruit.dto.RecruitRequestQuery;
import com.enterprise.hr.recruit.dto.RecruitRequestVO;
import com.enterprise.hr.recruit.dto.RequestResultVO;
import com.enterprise.hr.recruit.entity.RecruitRequest;
import com.enterprise.hr.recruit.mapper.RecruitRequestMapper;
import com.enterprise.hr.recruit.service.RecruitRequestService;
import com.enterprise.hr.recruit.service.support.RecruitSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 招聘需求业务实现（F-06-01 提报与查询 / F-06-02 HR 审批）。
 *
 * <p>流程：主管提报 → {@code approval_status = 1 审批中} → HR 审批；
 * 审批只改变审批状态，需求单的 {@code status} 由"入职推送"驱动
 * （录用满员时置 2 已完成，见 {@code OfferServiceImpl} BR-06-03）。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class RecruitRequestServiceImpl extends ServiceImpl<RecruitRequestMapper, RecruitRequest>
        implements RecruitRequestService {

    /** 需求状态：进行中 */
    private static final int REQUEST_ONGOING = 1;
    /** 紧急程度：普通 */
    private static final int URGENCY_NORMAL = 1;

    private final RecruitSupport recruitSupport;

    @Override
    public PageResult<RecruitRequestVO> pageRequests(RecruitRequestQuery query) {
        LambdaQueryWrapper<RecruitRequest> wrapper = Wrappers.lambdaQuery();
        if (query.getStatus() != null) {
            wrapper.eq(RecruitRequest::getStatus, query.getStatus());
        }
        if (query.getApprovalStatus() != null) {
            wrapper.eq(RecruitRequest::getApprovalStatus, query.getApprovalStatus());
        }
        // 契约要求按需求单号倒序（单号自带年份 + 流水，倒序即最新在前）
        wrapper.orderByDesc(RecruitRequest::getRequestNo);

        Page<RecruitRequest> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<RecruitRequestVO> records = new ArrayList<>(page.getRecords().size());
        for (RecruitRequest request : page.getRecords()) {
            RecruitRequestVO vo = new RecruitRequestVO();
            BeanUtils.copyProperties(request, vo);
            records.add(vo);
        }
        recruitSupport.fillRecruitRequestVos(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequestResultVO create(RecruitRequestCreateReq req) {
        LoginUser user = UserContext.require();
        Long applicantId = requireEmployeeId(user);

        RecruitRequest entity = new RecruitRequest();
        entity.setRequestNo(recruitSupport.nextRequestNo());
        entity.setRequestDeptId(req.getRequestDeptId());
        entity.setPositionId(req.getPositionId());
        entity.setPositionName(req.getPositionName());
        entity.setRecruitCount(req.getRecruitCount());
        entity.setHiredCount(0);
        entity.setJobLevel(req.getJobLevel());
        entity.setSalaryMin(req.getSalaryMin());
        entity.setSalaryMax(req.getSalaryMax());
        entity.setEducationReq(req.getEducationReq());
        entity.setExperienceYears(req.getExperienceYears());
        entity.setJobDescription(req.getJobDescription());
        entity.setJobRequirement(req.getJobRequirement());
        entity.setHopeArriveDate(req.getHopeArriveDate());
        entity.setUrgency(req.getUrgency() == null ? URGENCY_NORMAL : req.getUrgency());
        entity.setApplicantId(applicantId);
        entity.setApplyTime(LocalDateTime.now());
        // 提报即进入审批中，等待 HR 审批
        entity.setApprovalStatus(BizConst.APPROVAL_PENDING);
        entity.setStatus(REQUEST_ONGOING);
        baseMapper.insert(entity);

        return RequestResultVO.ok(entity.getId(), entity.getRequestNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, RecruitApprovalReq req) {
        int result = req.getResult();
        if (result != BizConst.APPROVAL_APPROVED && result != BizConst.APPROVAL_REJECTED) {
            throw BizException.badRequest("无效的审批结果");
        }
        RecruitRequest request = baseMapper.selectById(id);
        if (request == null) {
            throw BizException.notFound("需求单不存在");
        }
        if (!Objects.equals(request.getApprovalStatus(), BizConst.APPROVAL_PENDING)) {
            throw BizException.badRequest("该需求单不在审批中");
        }
        // 用 LambdaUpdateWrapper 才能真正把 current_approver_id 置空（updateById 会忽略 null 字段）
        baseMapper.update(null, Wrappers.<RecruitRequest>lambdaUpdate()
                .set(RecruitRequest::getApprovalStatus, result)
                .set(RecruitRequest::getCurrentApproverId, null)
                .eq(RecruitRequest::getId, request.getId()));
    }

    /** 当前登录账号必须关联员工档案，否则无法落库 applicant_id */
    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案");
        }
        return employeeId;
    }
}
