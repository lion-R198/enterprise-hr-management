package com.enterprise.hr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统角色 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 *
 * @author Enterprise HR Team
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 统计同编码角色数量，<b>包含逻辑已删除的记录</b>。
     *
     * <p>{@code sys_role.role_code} 上有唯一键，逻辑删除不会释放该值。
     * 与 {@link SysUserMapper#countByUsernameIncludeDeleted} 同理：
     * 只有绕过逻辑删除去查，才能区分"编码已被他人占用"与"编码曾被删除、不可复用"。
     */
    @Select("SELECT COUNT(1) FROM sys_role WHERE role_code = #{roleCode}")
    long countByRoleCodeIncludeDeleted(@Param("roleCode") String roleCode);
}
