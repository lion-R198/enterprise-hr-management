package com.enterprise.hr.salary.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.entity.SalaryStandard;
import com.enterprise.hr.employee.mapper.SalaryStandardMapper;
import com.enterprise.hr.salary.dto.SalaryStandardCreateReq;
import com.enterprise.hr.salary.dto.SalaryStandardVO;
import com.enterprise.hr.salary.service.SalaryStandardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 薪资标准业务实现。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class SalaryStandardServiceImpl implements SalaryStandardService {

    /** 岗位序列缺省值：2 技术序列（与前端表单缺省一致） */
    private static final int DEFAULT_POSITION_SEQ = 2;

    /** 缺省金额 0.00 */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SalaryStandardMapper salaryStandardMapper;

    @Override
    public PageResult<SalaryStandardVO> page(PageQuery query) {
        PageQuery pageQuery = query == null ? new PageQuery() : query;
        Page<SalaryStandard> page = pageQuery.toPage();
        IPage<SalaryStandard> result = salaryStandardMapper.selectPage(page,
                Wrappers.<SalaryStandard>lambdaQuery()
                        .orderByAsc(SalaryStandard::getStandardCode));
        return PageResult.of(result, this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalaryStandard create(SalaryStandardCreateReq req) {
        String standardCode = req.getStandardCode().trim();
        // 唯一键前置校验：standard_code 为 UK，并发下的漏网之鱼由 DuplicateKeyException 兜底
        Long duplicated = salaryStandardMapper.selectCount(
                Wrappers.<SalaryStandard>lambdaQuery().eq(SalaryStandard::getStandardCode, standardCode));
        if (duplicated != null && duplicated > 0) {
            throw new BizException(ErrorCode.CONFLICT, "标准编码已存在：" + standardCode);
        }

        SalaryStandard entity = new SalaryStandard();
        entity.setStandardCode(standardCode);
        entity.setStandardName(req.getStandardName().trim());
        entity.setPositionSeq(req.getPositionSeq() == null ? DEFAULT_POSITION_SEQ : req.getPositionSeq());
        entity.setJobLevel(StringUtils.hasText(req.getJobLevel()) ? req.getJobLevel().trim() : null);
        entity.setBaseSalary(amount(req.getBaseSalary()));
        entity.setPostSalary(amount(req.getPostSalary()));
        entity.setPerformanceSalary(amount(req.getPerformanceSalary()));
        entity.setAllowance(amount(req.getAllowance()));
        entity.setEffectiveDate(req.getEffectiveDate() == null ? LocalDate.now() : req.getEffectiveDate());
        entity.setStatus(BizConst.STATUS_ENABLED);
        salaryStandardMapper.insert(entity);
        return entity;
    }

    // ------------------------------------------------------------------ 内部

    /** 金额兜底与规整：空值按 0.00，统一 2 位小数 HALF_UP */
    private BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO_AMOUNT : value.setScale(2, RoundingMode.HALF_UP);
    }

    private SalaryStandardVO toVO(SalaryStandard entity) {
        return SalaryStandardVO.builder()
                .id(entity.getId())
                .standardCode(entity.getStandardCode())
                .standardName(entity.getStandardName())
                .positionSeq(entity.getPositionSeq())
                .jobLevel(entity.getJobLevel())
                .baseSalary(entity.getBaseSalary())
                .postSalary(entity.getPostSalary())
                .performanceSalary(entity.getPerformanceSalary())
                .allowance(entity.getAllowance())
                .effectiveDate(entity.getEffectiveDate())
                .status(entity.getStatus())
                .build();
    }
}
