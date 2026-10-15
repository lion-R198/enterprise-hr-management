package com.enterprise.hr.system.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.SysLogQuery;
import com.enterprise.hr.system.dto.vo.SysLoginLogVO;
import com.enterprise.hr.system.dto.vo.SysOperLogVO;

/**
 * 日志审计服务（F-09-06，全量留痕 NF-09）。
 *
 * <p>登录日志与操作日志都是<b>只增不改</b>的审计数据：本模块只提供分页查询，
 * 不提供任何修改/删除入口——可被编辑的审计日志等于没有审计。
 *
 * @author Enterprise HR Team
 */
public interface SysLogService {

    /** 登录日志分页（倒序，最近的在最前） */
    PageResult<SysLoginLogVO> pageLoginLogs(SysLogQuery query);

    /** 操作日志分页（倒序，最近的在最前） */
    PageResult<SysOperLogVO> pageOperLogs(SysLogQuery query);
}
