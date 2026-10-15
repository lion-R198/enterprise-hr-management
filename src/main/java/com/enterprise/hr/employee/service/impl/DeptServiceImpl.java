package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.dto.DeptSaveReq;
import com.enterprise.hr.employee.dto.vo.DeptTreeVO;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.service.DeptService;
import com.enterprise.hr.employee.service.support.DeptScopeSupport;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门业务实现。
 *
 * <p>层级用<b>物化路径</b> {@code dept_path}（如 {@code /1/2/5/}）维护：
 * 查子树一次 LIKE，不用递归；移动部门时同步重写整棵子树的路径与层级。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    /** 顶级部门层级 */
    private static final int ROOT_LEVEL = 1;
    /** 根部门的物化路径前缀 */
    private static final String PATH_SEPARATOR = "/";

    private final EmployeeMapper employeeMapper;
    private final EmployeeInfoSupport employeeInfoSupport;
    private final DeptScopeSupport deptScopeSupport;

    /** 组织架构树：一次部门查询 + 一次分组统计，内存拼树 */
    @Override
    public List<DeptTreeVO> tree() {
        List<Dept> all = baseMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .orderByAsc(Dept::getSortNo)
                .orderByAsc(Dept::getId));
        Map<Long, Long> counts = employeeInfoSupport.countByDept();

        // 用 LinkedHashMap 保持 sort_no 的遍历顺序，拼树后同层顺序仍然是 sort_no
        Map<Long, DeptTreeVO> nodes = new LinkedHashMap<>(all.size() * 2);
        for (Dept dept : all) {
            DeptTreeVO vo = DeptTreeVO.from(dept);
            vo.setEmployeeCount(counts.getOrDefault(dept.getId(), 0L));
            nodes.put(dept.getId(), vo);
        }

        List<DeptTreeVO> roots = new ArrayList<>();
        for (DeptTreeVO vo : nodes.values()) {
            DeptTreeVO parent = vo.getParentId() == null ? null : nodes.get(vo.getParentId());
            if (parent == null) {
                // 顶级部门；父节点被逻辑删除的"孤儿节点"也按顶级展示，避免整棵子树消失
                roots.add(vo);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(vo);
        }
        return roots;
    }

    /** 扁平列表（仅启用） */
    @Override
    public List<DeptTreeVO> listEnabled() {
        List<Dept> list = baseMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .eq(Dept::getStatus, BizConst.STATUS_ENABLED)
                .orderByAsc(Dept::getSortNo)
                .orderByAsc(Dept::getId));
        Map<Long, Long> counts = employeeInfoSupport.countByDept();
        List<DeptTreeVO> vos = new ArrayList<>(list.size());
        for (Dept dept : list) {
            DeptTreeVO vo = DeptTreeVO.from(dept);
            vo.setEmployeeCount(counts.getOrDefault(dept.getId(), 0L));
            vos.add(vo);
        }
        return vos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeptSaveReq req) {
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        String name = trim(req.getName());
        String deptCode = trim(req.getDeptCode());
        if (!StringUtils.hasText(name)) {
            throw BizException.badRequest("缺少必填字段：name");
        }
        if (!StringUtils.hasText(deptCode)) {
            throw BizException.badRequest("缺少必填字段：deptCode");
        }
        // BR-03-01：部门编码全局唯一
        Long codeCount = baseMapper.selectCount(
                Wrappers.<Dept>lambdaQuery().eq(Dept::getDeptCode, deptCode));
        if (codeCount != null && codeCount > 0) {
            throw new BizException(ErrorCode.DEPT_CODE_DUPLICATED, "部门编码已存在（BR-03-01）: " + deptCode);
        }

        Dept parent = null;
        if (req.getParentId() != null) {
            parent = baseMapper.selectById(req.getParentId());
            if (parent == null) {
                throw new BizException(ErrorCode.DEPT_NOT_FOUND, "上级部门不存在: " + req.getParentId());
            }
        }
        int level = parent == null ? ROOT_LEVEL : safeLevel(parent) + 1;
        // BR-03-03：层级不得超过 5 层
        if (level > BizConst.DEPT_MAX_LEVEL) {
            throw new BizException(ErrorCode.DEPT_LEVEL_EXCEEDED);
        }

        Dept entity = new Dept();
        entity.setDeptCode(deptCode);
        entity.setName(name);
        entity.setParentId(parent == null ? null : parent.getId());
        entity.setDeptType(req.getDeptType() == null ? BizConst.DEPT_TYPE_DEPT : req.getDeptType());
        entity.setDeptLevel(level);
        entity.setHeadcountLimit(req.getHeadcountLimit() == null ? 0 : req.getHeadcountLimit());
        entity.setPhone(trim(req.getPhone()));
        entity.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        entity.setLeaderId(req.getLeaderId());
        entity.setLeader(trim(req.getLeader()));
        entity.setStatus(req.getStatus() == null ? BizConst.STATUS_ENABLED : req.getStatus());
        // 物化路径依赖自增主键，先落库拿到 id 再回写
        entity.setDeptPath(PATH_SEPARATOR);
        baseMapper.insert(entity);

        String path = (parent == null ? PATH_SEPARATOR : parent.getDeptPath()) + entity.getId() + PATH_SEPARATOR;
        Dept patch = new Dept();
        patch.setId(entity.getId());
        patch.setDeptPath(path);
        baseMapper.updateById(patch);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeptSaveReq req) {
        if (id == null) {
            throw BizException.badRequest("缺少必填参数：id");
        }
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        Dept dept = baseMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ErrorCode.DEPT_NOT_FOUND, "部门不存在: " + id);
        }

        if (StringUtils.hasText(req.getName())) {
            String name = req.getName().trim();
            if (!name.equals(dept.getName())) {
                // BR-03-01：同级部门名称不可重复
                if (existsSiblingName(name, dept.getParentId(), dept.getId())) {
                    throw new BizException(ErrorCode.DEPT_NAME_DUPLICATED, "同级部门名称重复（BR-03-01）: " + name);
                }
                dept.setName(name);
            }
        }
        if (req.getPhone() != null) {
            dept.setPhone(trim(req.getPhone()));
        }
        if (req.getHeadcountLimit() != null) {
            dept.setHeadcountLimit(req.getHeadcountLimit());
        }
        if (req.getSortNo() != null) {
            dept.setSortNo(req.getSortNo());
        }
        if (req.getDeptType() != null) {
            dept.setDeptType(req.getDeptType());
        }
        if (req.getStatus() != null) {
            dept.setStatus(req.getStatus());
        }
        if (req.getLeaderId() != null) {
            dept.setLeaderId(req.getLeaderId());
        }
        if (req.getLeader() != null) {
            dept.setLeader(trim(req.getLeader()));
        }
        // 部门编码是外部系统/报表引用的业务主键，不允许修改（变更请新建部门后转移）
        if (req.getParentId() != null && !req.getParentId().equals(dept.getParentId())) {
            move(dept, req.getParentId());
        }
        baseMapper.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw BizException.badRequest("缺少必填参数：id");
        }
        Dept dept = baseMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ErrorCode.DEPT_NOT_FOUND, "部门不存在: " + id);
        }
        Long children = baseMapper.selectCount(Wrappers.<Dept>lambdaQuery().eq(Dept::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        // BR-03-02：部门下存在在职员工（status != 0）禁止删除
        Long employees = employeeMapper.selectCount(Wrappers.<Employee>lambdaQuery()
                .eq(Employee::getDeptId, id)
                .ne(Employee::getStatus, BizConst.EMPLOYEE_RESIGNED));
        if (employees != null && employees > 0) {
            throw new BizException(ErrorCode.DEPT_HAS_EMPLOYEE);
        }
        baseMapper.deleteById(id);
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 调整部门的上级（局部更新时传入 parentId）。
     *
     * <p>校验：上级必须存在；不能挂到自己的子孙下（环）；移动后整棵子树的层级不得超过 5。
     * 通过后同步重写自身与所有下级的 {@code dept_path} 与 {@code dept_level}。
     */
    private void move(Dept dept, Long newParentId) {
        Dept parent = baseMapper.selectById(newParentId);
        if (parent == null) {
            throw new BizException(ErrorCode.DEPT_NOT_FOUND, "上级部门不存在: " + newParentId);
        }
        // 禁止把部门挂到自己的子孙下：物化路径可一次性判定
        if (deptScopeSupport.isSelfOrDescendant(dept.getDeptPath(), dept.getId(), newParentId)) {
            throw BizException.badRequest("不能将部门移动到其下级部门下（构成环）");
        }
        int newLevel = safeLevel(parent) + 1;

        String oldPath = dept.getDeptPath();
        String newPath = parent.getDeptPath() + dept.getId() + PATH_SEPARATOR;
        if (!StringUtils.hasText(oldPath)) {
            // 脏数据兜底：没有物化路径时只更新自身
            dept.setParentId(newParentId);
            dept.setDeptLevel(newLevel);
            dept.setDeptPath(newPath);
            return;
        }
        List<Dept> subtree = baseMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .likeRight(Dept::getDeptPath, oldPath));
        int maxLevel = safeLevel(dept);
        for (Dept node : subtree) {
            maxLevel = Math.max(maxLevel, safeLevel(node));
        }
        int delta = newLevel - safeLevel(dept);
        if (maxLevel + delta > BizConst.DEPT_MAX_LEVEL) {
            // BR-03-03：移动后子树会超出 5 层
            throw new BizException(ErrorCode.DEPT_LEVEL_EXCEEDED);
        }
        for (Dept node : subtree) {
            String suffix = node.getDeptPath().substring(oldPath.length());
            Dept patch = new Dept();
            patch.setId(node.getId());
            patch.setDeptPath(newPath + suffix);
            patch.setDeptLevel(safeLevel(node) + delta);
            baseMapper.updateById(patch);
        }
        dept.setParentId(newParentId);
        dept.setDeptLevel(newLevel);
        dept.setDeptPath(newPath);
    }

    /** 同级部门名是否已被占用（排除自身） */
    private boolean existsSiblingName(String name, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Dept> wrapper = Wrappers.<Dept>lambdaQuery()
                .eq(Dept::getName, name)
                .ne(Dept::getId, excludeId);
        if (parentId == null) {
            wrapper.isNull(Dept::getParentId);
        } else {
            wrapper.eq(Dept::getParentId, parentId);
        }
        Long count = baseMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private static int safeLevel(Dept dept) {
        return dept.getDeptLevel() == null ? ROOT_LEVEL : dept.getDeptLevel();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
