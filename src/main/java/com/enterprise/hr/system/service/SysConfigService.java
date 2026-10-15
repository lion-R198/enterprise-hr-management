package com.enterprise.hr.system.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.SysConfigQuery;
import com.enterprise.hr.system.dto.SysConfigUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysConfigVO;

/**
 * 系统参数服务（F-09-05，业务规则可配置化 NF-18）。
 *
 * <p>参数化是"业务规则可配置"的落点：考勤封账日、迟到容忍分钟数、个税起征点等
 * 都从 {@code sys_config} 读取而非硬编码，政策一变改配置即可，无需发版。
 * 正因为它是业务规则的输入，修改必须做<b>值的语义校验</b>——把封账日改成 31
 * 会让 2 月永远无法封账，这类"合法但不合理"的值必须挡住。
 *
 * @author Enterprise HR Team
 */
public interface SysConfigService {

    /** 参数列表（可按分组筛选，返回全集不分页） */
    PageResult<SysConfigVO> list(SysConfigQuery query);

    /** 修改参数值（含值类型与业务区间校验） */
    OpResultVO update(Long id, SysConfigUpdateReq req);
}
