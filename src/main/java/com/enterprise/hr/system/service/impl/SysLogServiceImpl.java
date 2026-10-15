package com.enterprise.hr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.SysLogQuery;
import com.enterprise.hr.system.dto.vo.SysLoginLogVO;
import com.enterprise.hr.system.dto.vo.SysOperLogVO;
import com.enterprise.hr.system.entity.SysLoginLog;
import com.enterprise.hr.system.entity.SysOperLog;
import com.enterprise.hr.system.mapper.SysLoginLogMapper;
import com.enterprise.hr.system.mapper.SysOperLogMapper;
import com.enterprise.hr.system.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 日志审计服务实现。
 *
 * <p>两个查询都强制按时间倒序：审计的第一诉求是"最近发生了什么"，
 * 允许客户端指定排序会让默认视图失去意义。
 *
 * @author Enterprise HR Team
 */
@Service
public class SysLogServiceImpl implements SysLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;

    public SysLogServiceImpl(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operLogMapper = operLogMapper;
    }

    @Override
    public PageResult<SysLoginLogVO> pageLoginLogs(SysLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.<SysLoginLog>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysLoginLog::getUsername, trim(query.getUsername()))
                .eq(query.getStatus() != null, SysLoginLog::getLoginStatus, query.getStatus())
                .ge(query.getStartDate() != null, SysLoginLog::getLoginTime, startOfDay(query))
                .le(query.getEndDate() != null, SysLoginLog::getLoginTime, endOfDay(query))
                .orderByDesc(SysLoginLog::getLoginTime)
                .orderByDesc(SysLoginLog::getId);

        IPage<SysLoginLog> page = loginLogMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, this::toLoginVO);
    }

    @Override
    public PageResult<SysOperLogVO> pageOperLogs(SysLogQuery query) {
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.<SysOperLog>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysOperLog::getUsername, trim(query.getUsername()))
                .like(StringUtils.hasText(query.getModule()), SysOperLog::getModule, trim(query.getModule()))
                .eq(query.getStatus() != null, SysOperLog::getOperStatus, query.getStatus())
                .ge(query.getStartDate() != null, SysOperLog::getOperTime, startOfDay(query))
                .le(query.getEndDate() != null, SysOperLog::getOperTime, endOfDay(query))
                .orderByDesc(SysOperLog::getOperTime)
                .orderByDesc(SysOperLog::getId);

        IPage<SysOperLog> page = operLogMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page, this::toOperVO);
    }

    // ------------------------------------------------------------------ 内部

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 起始日期归零到 00:00:00，让「查 9 月 1 日」包含当天全部记录 */
    private LocalDateTime startOfDay(SysLogQuery query) {
        return query.getStartDate() == null ? null : query.getStartDate().atStartOfDay();
    }

    /** 结束日期推到 23:59:59.999999999，避免同一天的记录被 {@code le} 排除 */
    private LocalDateTime endOfDay(SysLogQuery query) {
        return query.getEndDate() == null ? null : query.getEndDate().atTime(LocalTime.MAX);
    }

    private SysLoginLogVO toLoginVO(SysLoginLog entity) {
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setLoginIp(entity.getLoginIp());
        vo.setLoginLocation(entity.getLoginLocation());
        vo.setBrowser(entity.getBrowser());
        vo.setOs(entity.getOs());
        vo.setLoginStatus(entity.getLoginStatus());
        vo.setMessage(entity.getMessage());
        vo.setLoginTime(entity.getLoginTime());
        return vo;
    }

    private SysOperLogVO toOperVO(SysOperLog entity) {
        SysOperLogVO vo = new SysOperLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperType(entity.getOperType());
        vo.setOperDesc(entity.getOperDesc());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setRequestUrl(entity.getRequestUrl());
        vo.setOperIp(entity.getOperIp());
        vo.setOperStatus(entity.getOperStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCostTime(entity.getCostTime());
        vo.setOperTime(entity.getOperTime());
        return vo;
    }
}
