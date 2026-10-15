package com.enterprise.hr.system.service;

import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.SysRoleSaveReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysRoleVO;

/**
 * 角色权限服务（F-09-02）。
 *
 * <p>角色的 {@code data_scope} 决定行级数据范围（NF-10），是"能看到哪些行"的唯一开关，
 * 因此角色维护比普通字典更严格：内置角色（{@code role_type = 1}）的编码不可改。
 *
 * @author Enterprise HR Team
 */
public interface SysRoleService {

    /**
     * 角色列表。
     *
     * <p>角色是典型的"小数据集、强排序"场景（全系统也只有个位数到几十条），
     * 一次全量返回并按 {@code sort_no} 排序，前端下拉框与列表页共用同一接口。
     *
     * @param query 分页参数（不传则返回全部）
     */
    PageResult<SysRoleVO> list(PageQuery query);

    /** 新增角色 */
    OpResultVO create(SysRoleSaveReq req);

    /** 修改角色（内置角色编码不可变更） */
    OpResultVO update(Long id, SysRoleSaveReq req);
}
