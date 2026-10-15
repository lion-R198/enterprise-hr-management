package com.enterprise.hr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.SysConfig;

/**
 * 系统参数 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 *
 * @author Enterprise HR Team
 */
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
