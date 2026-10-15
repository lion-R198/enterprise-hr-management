package com.enterprise.hr.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.entity.SysConfig;
import com.enterprise.hr.system.dto.SysConfigQuery;
import com.enterprise.hr.system.dto.SysConfigUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysConfigVO;
import com.enterprise.hr.system.mapper.SysConfigMapper;
import com.enterprise.hr.system.service.SysConfigService;
import com.enterprise.hr.system.support.ConfigValueValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统参数服务实现。
 *
 * @author Enterprise HR Team
 */
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;

    public SysConfigServiceImpl(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public PageResult<SysConfigVO> list(SysConfigQuery query) {
        List<SysConfigVO> list = configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                        .eq(query != null && StringUtils.hasText(query.getGroup()),
                                SysConfig::getConfigGroup,
                                query == null ? null : query.getGroup())
                        .orderByAsc(SysConfig::getConfigGroup)
                        .orderByAsc(SysConfig::getId))
                .stream().map(this::toVO).toList();
        return PageResult.list(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO update(Long id, SysConfigUpdateReq req) {
        SysConfig config = id == null ? null : configMapper.selectById(id);
        if (config == null) {
            throw BizException.notFound("参数不存在");
        }
        // 校验放在写库之前：参数值是业务规则的输入，非法值一旦落库，
        // 后续考勤封账、薪资核算会在不可预期的位置报错，排查成本远高于此处拦截
        ConfigValueValidator.validate(config.getConfigKey(), config.getConfigName(),
                req.getConfigValue(), config.getValueType(), config.getIsSystem());

        SysConfig patch = new SysConfig();
        patch.setId(id);
        patch.setConfigValue(StringUtils.hasText(req.getConfigValue()) ? req.getConfigValue().trim() : req.getConfigValue());
        configMapper.updateById(patch);
        return OpResultVO.ok();
    }

    private SysConfigVO toVO(SysConfig config) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigName(config.getConfigName());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigGroup(config.getConfigGroup());
        vo.setValueType(config.getValueType());
        vo.setIsSystem(config.getIsSystem());
        vo.setRemark(config.getRemark());
        return vo;
    }
}
