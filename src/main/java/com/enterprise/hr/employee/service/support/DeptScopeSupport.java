package com.enterprise.hr.employee.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.mapper.DeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 部门范围解析支持（NF-10 行级数据范围）。
 *
 * <p>部门主管的可见范围是"本部门及其所有下级部门"，部门表用<b>物化路径</b>
 * {@code dept_path}（如 {@code /1/2/5/}）表达层级，因此只需一条 SQL：
 * {@code dept_path LIKE '/1/2/%'}。相比递归查询（每层一次 SQL）或全表加载后
 * 在内存里拼树，物化路径在一次查询内就拿到整棵子树。
 *
 * <p>路径以 {@code /} 结尾，天然避免前缀误命中：
 * {@code '/1/2/'} 不会匹配 {@code '/1/20/'}（第 5 个字符 '/' 与 '0' 不同）。
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class DeptScopeSupport {

    private final DeptMapper deptMapper;

    /**
     * 取本部门及全部下级部门 ID（含自身）。
     *
     * <p><b>空列表语义</b>：当前用户没有部门（或部门已被删除）→ 无任何可见数据，
     * 调用方应据此生成"查不到数据"的 SQL 条件，而不是不加条件
     * ——后者会让"没有部门的主管"看到全量数据，属于越权。
     */
    public List<Long> deptAndDescendantIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        Dept self = deptMapper.selectById(deptId);
        if (self == null) {
            return Collections.emptyList();
        }
        String path = self.getDeptPath();
        if (!StringUtils.hasText(path)) {
            // 脏数据兜底：没有物化路径时至少保证能看到本部门
            return Collections.singletonList(deptId);
        }
        List<Dept> subtree = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .select(Dept::getId)
                .likeRight(Dept::getDeptPath, path));
        if (subtree == null || subtree.isEmpty()) {
            return Collections.singletonList(deptId);
        }
        return subtree.stream().map(Dept::getId).toList();
    }

    /**
     * 判断 {@code targetDeptId} 是否落在 {@code ancestorDeptId} 的子树内（含自身）。
     * 用于按 ID 直查的详情接口做行级越权拦截。
     */
    public boolean isInSubtree(Long ancestorDeptId, Long targetDeptId) {
        if (ancestorDeptId == null || targetDeptId == null) {
            return false;
        }
        if (ancestorDeptId.equals(targetDeptId)) {
            return true;
        }
        return deptAndDescendantIds(ancestorDeptId).contains(targetDeptId);
    }

    /**
     * 判断 {@code candidateParentId} 是否是 {@code deptId} 的自身或子孙。
     * 用于"禁止把部门挂到自己的子孙下（构成环）"的校验。
     *
     * @param deptPath       被移动部门的物化路径
     * @param deptId         被移动部门 ID
     * @param candidateParentId 目标上级部门 ID
     */
    public boolean isSelfOrDescendant(String deptPath, Long deptId, Long candidateParentId) {
        if (candidateParentId == null) {
            return false;
        }
        if (candidateParentId.equals(deptId)) {
            return true;
        }
        if (!StringUtils.hasText(deptPath)) {
            return false;
        }
        Dept candidate = deptMapper.selectById(candidateParentId);
        return candidate != null
                && StringUtils.hasText(candidate.getDeptPath())
                && candidate.getDeptPath().startsWith(deptPath);
    }
}
