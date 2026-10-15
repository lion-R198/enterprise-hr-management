package com.enterprise.hr.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 *
 * @author Enterprise HR Team
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 统计同名账号数量，<b>包含逻辑已删除的记录</b>。
     *
     * <p>为什么需要这个"绕过逻辑删除"的查询：{@code sys_user.username} 上有唯一键，
     * 而逻辑删除只把 {@code deleted} 置 1、<b>并不释放该唯一值</b>。因此
     * {@link BaseMapper#selectCount} 自动追加的 {@code deleted = 0} 会得出"账号可用"的结论，
     * 但随后的 INSERT 仍会被唯一键拦下，最终抛出一个语义模糊的"数据已存在"。
     *
     * <p>自定义 SQL 不会被 MyBatis-Plus 追加逻辑删除条件，因此能查到真实的占用情况，
     * 使系统可以给出"该账号名曾被删除、不可复用"这样可执行的提示。
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    long countByUsernameIncludeDeleted(@Param("username") String username);
}
