package com.enterprise.hr.recruit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.dto.InterviewRecordCreateReq;
import com.enterprise.hr.recruit.dto.InterviewRecordQuery;
import com.enterprise.hr.recruit.dto.InterviewRecordVO;
import com.enterprise.hr.recruit.entity.Candidate;
import com.enterprise.hr.recruit.entity.InterviewRecord;
import com.enterprise.hr.recruit.mapper.CandidateMapper;
import com.enterprise.hr.recruit.mapper.InterviewRecordMapper;
import com.enterprise.hr.recruit.service.InterviewService;
import com.enterprise.hr.recruit.service.support.RecruitSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试记录业务实现（F-06-04）。
 *
 * <p>两条业务联动：
 * <ul>
 *     <li>轮次自动递增（该候选人已有面试记录数 + 1），前端不传轮次，避免人工填错</li>
 *     <li>面试结论联动候选人阶段：通过 → 至少推进到"面试中"；淘汰 → 直接进入"已淘汰"</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl extends ServiceImpl<InterviewRecordMapper, InterviewRecord>
        implements InterviewService {

    /** 面试结论：通过 */
    private static final int CONCLUSION_PASS = 1;
    /** 面试结论：淘汰 */
    private static final int CONCLUSION_REJECT = 3;
    /** 入职前的"待录用"阶段阈值：阶段 < 4 时才允许被"通过"抬升 */
    private static final int STAGE_BEFORE_OFFER = BizConst.CANDIDATE_OFFER;

    private final CandidateMapper candidateMapper;
    private final RecruitSupport recruitSupport;

    @Override
    public PageResult<InterviewRecordVO> listRecords(InterviewRecordQuery query) {
        LambdaQueryWrapper<InterviewRecord> wrapper = Wrappers.lambdaQuery();
        if (query.getCandidateId() != null) {
            wrapper.eq(InterviewRecord::getCandidateId, query.getCandidateId());
        }
        wrapper.orderByDesc(InterviewRecord::getInterviewTime).orderByDesc(InterviewRecord::getId);

        List<InterviewRecord> list = baseMapper.selectList(wrapper);
        List<InterviewRecordVO> records = new ArrayList<>(list.size());
        for (InterviewRecord record : list) {
            InterviewRecordVO vo = new InterviewRecordVO();
            BeanUtils.copyProperties(record, vo);
            records.add(vo);
        }
        recruitSupport.fillInterviewVos(records);
        // 非分页接口统一走 PageResult.list，前端仍读 records / total
        return PageResult.list(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdResultVO create(InterviewRecordCreateReq req) {
        Candidate candidate = candidateMapper.selectById(req.getCandidateId());
        if (candidate == null) {
            throw BizException.notFound("候选人不存在");
        }
        Long prevCount = baseMapper.selectCount(Wrappers.<InterviewRecord>lambdaQuery()
                .eq(InterviewRecord::getCandidateId, candidate.getId()));

        InterviewRecord entity = new InterviewRecord();
        entity.setCandidateId(candidate.getId());
        entity.setRecruitRequestId(candidate.getRecruitRequestId());
        entity.setRoundNo((prevCount == null ? 0 : prevCount.intValue()) + 1);
        entity.setInterviewType(req.getInterviewType());
        entity.setInterviewTime(req.getInterviewTime());
        entity.setInterviewPlace(req.getInterviewPlace());
        entity.setInterviewerId(req.getInterviewerId());
        entity.setScore(req.getScore());
        entity.setEvaluation(req.getEvaluation());
        entity.setConclusion(req.getConclusion());
        baseMapper.insert(entity);

        applyConclusionToStage(candidate, req.getConclusion());
        return IdResultVO.ok(entity.getId());
    }

    /**
     * 面试结论联动候选人阶段。
     *
     * <p>通过：候选人进入"面试中"（不回退更靠后的阶段）；
     * 淘汰：直接置"已淘汰"（终态，与 BR-06-01 的单向流转一致）。
     */
    private void applyConclusionToStage(Candidate candidate, Integer conclusion) {
        if (conclusion == null) {
            return;
        }
        int current = candidate.getStage() == null ? BizConst.CANDIDATE_NEW : candidate.getStage();
        Integer target = null;
        if (conclusion == CONCLUSION_PASS && current < STAGE_BEFORE_OFFER) {
            target = BizConst.CANDIDATE_INTERVIEWING;
        } else if (conclusion == CONCLUSION_REJECT) {
            target = BizConst.CANDIDATE_ELIMINATED;
        }
        if (target == null || target.equals(current)) {
            return;
        }
        LambdaUpdateWrapper<Candidate> update = Wrappers.<Candidate>lambdaUpdate()
                .set(Candidate::getStage, target)
                .eq(Candidate::getId, candidate.getId());
        candidateMapper.update(null, update);
    }
}
