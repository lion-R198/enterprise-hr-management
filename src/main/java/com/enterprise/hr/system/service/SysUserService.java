package com.enterprise.hr.system.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.SysUserCreateReq;
import com.enterprise.hr.system.dto.SysUserQuery;
import com.enterprise.hr.system.dto.SysUserUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysUserVO;

/**
 * 系统用户服务（F-09-01 用户管理）。
 *
 * <p>与员工档案的分工：{@code employee} 管"人"，{@code sys_user} 管"账号"。
 * 一个人可以没有账号（外包/离职人员保留档案），一个账号也可以不绑定员工
 * （纯运维账号），因此两者是<b>弱关联</b>（{@code sys_user.employee_id} 可为空）。
 *
 * @author Enterprise HR Team
 */
public interface SysUserService {

    /** 分页查询账号，含关联员工姓名、部门名称与角色名 */
    PageResult<SysUserVO> page(SysUserQuery query);

    /** 新增账号（BR-09-01：初始密码不少于 8 位） */
    OpResultVO create(SysUserCreateReq req);

    /** 修改账号（局部更新；BR-09-02：管理员账号保护） */
    OpResultVO update(Long id, SysUserUpdateReq req);

    /** 重置密码为系统初始密码，并解除锁定 */
    OpResultVO resetPassword(Long id);

    /** 删除账号（逻辑删除；BR-09-02：管理员账号不可删除） */
    OpResultVO delete(Long id);
}
