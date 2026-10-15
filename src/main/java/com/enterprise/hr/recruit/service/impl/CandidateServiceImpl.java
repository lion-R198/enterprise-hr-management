package com.enterprise.hr.recruit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.recruit.dto.CandidateCreateReq;
import com.enterprise.hr.recruit.dto.CandidateQuery;
import com.enterprise.hr.recruit.dto.CandidateStageReq;
import com.enterprise.hr.recruit.dto.CandidateVO;
import com.enterprise.hr.recruit.dto.IdResultVO;
import com.enterprise.hr.recruit.entity.Candidate;
import com.enterprise.hr.recruit.mapper.CandidateMapper;
import com.enterprise.hr.recruit.service.CandidateService;
import com.enterprise.hr.recruit.service.support.RecruitSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 候选人业务实现（F-06-03）。
 *
 * <p>候选人阶段是招聘漏斗的唯一状态位：1 待筛选 → 2 待面试 → 3 面试中 → 4 待录用
 * → 5 已录用，任一阶段可跳到 6 已淘汰；<b>只允许单向推进</b>（BR-06-01）。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class CandidateServiceImpl extends ServiceImpl<CandidateMapper, Candidate>
        implements CandidateService {

    /** 简历来源：招聘网站（缺省） */
    private static final int SOURCE_DEFAULT = 2;
    /** 性别：未知 */
    private static final String GENDER_UNKNOWN = "U";

    private final RecruitSupport recruitSupport;

    @Override
    public PageResult<CandidateVO> pageCandidates(CandidateQuery query) {
        LambdaQueryWrapper<Candidate> wrapper = Wrappers.lambdaQuery();
        if (query.getRecruitRequestId() != null) {
            wrapper.eq(Candidate::getRecruitRequestId, query.getRecruitRequestId());
        }
        if (query.getStage() != null) {
            wrapper.eq(Candidate::getStage, query.getStage());
        }
        if (query.getIsTalent() != null) {
            wrapper.eq(Candidate::getIsTalent, query.getIsTalent());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Candidate::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(Candidate::getId);

        Page<Candidate> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<CandidateVO> records = new ArrayList<>(page.getRecords().size());
        for (Candidate candidate : page.getRecords()) {
            CandidateVO vo = new CandidateVO();
            BeanUtils.copyProperties(candidate, vo);
            records.add(vo);
        }
        recruitSupport.fillCandidateVos(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdResultVO create(CandidateCreateReq req) {
        // 关联需求必须存在，否则候选人无法归属招聘漏斗（外键约束会以 500 暴露，这里提前拦截为 400/404）
        if (recruitSupport.loadRecruitRequests(List.of(req.getRecruitRequestId())).isEmpty()) {
            throw BizException.notFound("招聘需求不存在");
        }

        Candidate entity = new Candidate();
        entity.setRecruitRequestId(req.getRecruitRequestId());
        entity.setName(req.getName());
        entity.setGender(req.getGender() == null ? GENDER_UNKNOWN : req.getGender());
        entity.setEducation(req.getEducation());
        entity.setGraduateSchool(req.getGraduateSchool());
        entity.setWorkYears(req.getWorkYears());
        entity.setCurrentCompany(req.getCurrentCompany());
        entity.setCurrentPosition(req.getCurrentPosition());
        entity.setExpectSalary(req.getExpectSalary());
        entity.setSource(req.getSource() == null ? SOURCE_DEFAULT : req.getSource());
        entity.setPhone(req.getPhone());
        entity.setEmail(req.getEmail());
        entity.setRemark(req.getRemark());
        // 新录入候选人：待筛选，不自动入人才库
        entity.setStage(BizConst.CANDIDATE_NEW);
        entity.setIsTalent(0);
        baseMapper.insert(entity);

        return IdResultVO.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStage(Long id, CandidateStageReq req) {
        Candidate candidate = baseMapper.selectById(id);
        if (candidate == null) {
            throw BizException.notFound("候选人不存在");
        }
        int to = req.getStage();
        if (to < BizConst.CANDIDATE_NEW || to > BizConst.CANDIDATE_ELIMINATED) {
            throw BizException.badRequest("无效的候选人阶段");
        }
        int current = candidate.getStage() == null ? BizConst.CANDIDATE_NEW : candidate.getStage();
        // BR-06-01 阶段单向流转：禁止回退；淘汰(6)属于终态，任何阶段都允许直接进入
        if (to < current && to != BizConst.CANDIDATE_ELIMINATED) {
            throw new BizException(ErrorCode.CANDIDATE_STAGE_ROLLBACK);
        }

        LambdaUpdateWrapper<Candidate> update = Wrappers.<Candidate>lambdaUpdate()
                .set(Candidate::getStage, to)
                .eq(Candidate::getId, candidate.getId());
        // 淘汰时同步记录淘汰原因（与前端 mock 一致：仅淘汰动作回写 remark）
        if (to == BizConst.CANDIDATE_ELIMINATED && req.getRemark() != null && !req.getRemark().isBlank()) {
            update.set(Candidate::getRemark, req.getRemark());
        }
        baseMapper.update(null, update);
    }
}
