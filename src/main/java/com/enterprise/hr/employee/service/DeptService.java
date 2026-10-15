package com.enterprise.hr.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.employee.dto.DeptSaveReq;
import com.enterprise.hr.employee.dto.vo.DeptTreeVO;
import com.enterprise.hr.employee.entity.Dept;

import java.util.List;

/**
 * 部门业务接口（F-03-01 组织架构 / F-03-02 部门维护 / F-03-04 编制占用）。
 *
 * <p>涉及业务规则：
 * <ul>
 *     <li>BR-03-01 部门编码全局唯一、同级部门名称不可重复</li>
 *     <li>BR-03-02 部门下存在在职员工时禁止删除</li>
 *     <li>BR-03-03 部门层级不得超过 5 层</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
public interface DeptService extends IService<Dept> {

    /** 组织架构树（每个节点补在岗人数 employeeCount） */
    List<DeptTreeVO> tree();

    /** 启用状态的部门扁平列表（下拉框用），按 sort_no 升序 */
    List<DeptTreeVO> listEnabled();

    /**
     * 新增部门。
     *
     * <p>BR-03-01 编码唯一；上级不存在抛 404；BR-03-03 层级不超过 5 层；
     * 维护物化路径 {@code dept_path = 上级path + 本id + "/"}（根部门为 {@code /{id}/}）。
     *
     * @return 新部门 ID
     */
    Long create(DeptSaveReq req);

    /**
     * 修改部门（局部更新，未传字段保持原值；部门编码不可修改）。
     *
     * <p>BR-03-01 同级部门名称不可重复；禁止把部门挂到自己的子孙下（构成环）。
     */
    void update(Long id, DeptSaveReq req);

    /**
     * 删除部门（逻辑删除）。
     *
     * <p>存在下级部门 → 400；BR-03-02 部门下有在职员工 → 400。
     */
    void delete(Long id);
}
