package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.dto.EmployeeContractCreateReq;
import com.enterprise.hr.employee.dto.EmployeeContractQuery;
import com.enterprise.hr.employee.dto.vo.EmployeeContractVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.EmployeeContract;
import com.enterprise.hr.employee.entity.SysConfig;
import com.enterprise.hr.employee.mapper.EmployeeContractMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.service.EmployeeContractService;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import com.enterprise.hr.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 劳动合同业务实现。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class EmployeeContractServiceImpl extends ServiceImpl<EmployeeContractMapper, EmployeeContract>
        implements EmployeeContractService {

    /** 合同编号前缀（CT = ConTract） */
    private static final String CONTRACT_NO_PREFIX = "CT";
    /** 甲方公司名兜底常量（{@code sys_config} 未配置公司名时使用，与前端 mock 一致） */
    private static final String DEFAULT_COMPANY_NAME = "华信科技集团";
    /** 甲方公司名的系统参数键（按优先级探测） */
    private static final String[] COMPANY_NAME_KEYS = {"company.name", "system.company.name", "company_name"};
    /** 默认合同类型：1 固定期限 */
    private static final int DEFAULT_CONTRACT_TYPE = 1;
    /** 合同状态：1 生效中 */
    private static final int CONTRACT_STATUS_ACTIVE = 1;
    /** 合同编号自动生成的最大重试次数（并发撞号兜底） */
    private static final int CONTRACT_NO_MAX_RETRY = 3;

    private final EmployeeMapper employeeMapper;
    private final EmployeeInfoSupport employeeInfoSupport;
    private final SysConfigMapper sysConfigMapper;

    @Override
    public PageResult<EmployeeContractVO> page(EmployeeContractQuery query) {
        EmployeeContractQuery q = query == null ? new EmployeeContractQuery() : query;

        LambdaQueryWrapper<EmployeeContract> wrapper = Wrappers.<EmployeeContract>lambdaQuery();
        if (q.getEmployeeId() != null) {
            wrapper.eq(EmployeeContract::getEmployeeId, q.getEmployeeId());
        }
        if (q.getStatus() != null) {
            wrapper.eq(EmployeeContract::getStatus, q.getStatus());
        }
        wrapper.orderByDesc(EmployeeContract::getStartDate).orderByDesc(EmployeeContract::getId);

        IPage<EmployeeContract> page = baseMapper.selectPage(q.toPage(), wrapper);
        List<EmployeeContractVO> vos = new ArrayList<>();
        List<Long> employeeIds = new ArrayList<>();
        if (page.getRecords() != null) {
            for (EmployeeContract contract : page.getRecords()) {
                EmployeeContractVO vo = EmployeeContractVO.from(contract);
                vos.add(vo);
                employeeIds.add(vo.getEmployeeId());
            }
        }
        Map<Long, String> employeeNames = employeeInfoSupport.loadEmployeeNames(employeeIds);
        for (EmployeeContractVO vo : vos) {
            vo.setEmployeeName(employeeNames.get(vo.getEmployeeId()));
        }
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeContract create(EmployeeContractCreateReq req) {
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        if (req.getEmployeeId() == null) {
            throw BizException.badRequest("缺少必填字段：employeeId");
        }
        if (req.getStartDate() == null) {
            throw BizException.badRequest("缺少必填字段：startDate");
        }
        Employee employee = employeeMapper.selectById(req.getEmployeeId());
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + req.getEmployeeId());
        }
        if (req.getSalaryAgreed() != null && req.getSalaryAgreed().signum() < 0) {
            throw new BizException(ErrorCode.NEGATIVE_SALARY);
        }

        EmployeeContract entity = new EmployeeContract();
        entity.setContractNo(nextContractNo());
        entity.setEmployeeId(employee.getId());
        entity.setContractType(req.getContractType() == null ? DEFAULT_CONTRACT_TYPE : req.getContractType());
        entity.setFirstParty(companyName());
        // 乙方取员工姓名快照：员工改名不影响已签订合同的主体
        entity.setSecondParty(employee.getName());
        entity.setSignDate(req.getSignDate() == null ? LocalDate.now() : req.getSignDate());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setTermYears(req.getTermYears());
        entity.setSalaryAgreed(req.getSalaryAgreed());
        entity.setProbationMonths(req.getProbationMonths() == null ? 0 : req.getProbationMonths());
        entity.setStatus(CONTRACT_STATUS_ACTIVE);
        entity.setRemark(req.getRemark() == null ? null : req.getRemark().trim());
        insertWithContractNoRetry(entity);
        return entity;
    }

    // ------------------------------------------------------------------ 内部

    /** 甲方公司名：优先取系统参数，未配置时用内置常量 */
    private String companyName() {
        List<SysConfig> configs = sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .in(SysConfig::getConfigKey, List.of(COMPANY_NAME_KEYS)));
        for (SysConfig config : configs) {
            if (StringUtils.hasText(config.getConfigValue())) {
                return config.getConfigValue().trim();
            }
        }
        return DEFAULT_COMPANY_NAME;
    }

    /** 生成合同编号：{@code CT + 年份 + 5 位流水}，流水取当年已有合同数 + 1 */
    private String nextContractNo() {
        Long exists = baseMapper.selectCount(Wrappers.<EmployeeContract>lambdaQuery()
                .likeRight(EmployeeContract::getContractNo, BizNoGenerator.yearPrefix(CONTRACT_NO_PREFIX)));
        long seq = (exists == null ? 0L : exists) + 1;
        return BizNoGenerator.next(CONTRACT_NO_PREFIX, seq);
    }

    /**
     * 插入合同，编号唯一键冲突时重新取号重试。
     *
     * <p>并发签订时"当年合同数 + 1"可能算出同一编号，由唯一键拦下后一条，
     * 捕获后重新取号即可；MySQL 重复键错误只回滚当前语句，事务未被污染。
     */
    private void insertWithContractNoRetry(EmployeeContract entity) {
        int attempt = 0;
        while (true) {
            try {
                baseMapper.insert(entity);
                return;
            } catch (DuplicateKeyException e) {
                if (++attempt >= CONTRACT_NO_MAX_RETRY) {
                    throw e;
                }
                entity.setId(null);
                entity.setContractNo(nextContractNo());
            }
        }
    }
}
