package com.enterprise.hr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.SysRoleMenu;

/**
 * 角色菜单关联 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 *
 * @author Enterprise HR Team
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
