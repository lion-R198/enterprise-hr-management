package com.enterprise.hr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.DataScopeType;
import com.enterprise.hr.employee.entity.SysRole;
import com.enterprise.hr.system.dto.SysRoleSaveReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysRoleVO;
import com.enterprise.hr.system.mapper.SysRoleMapper;
import com.enterprise.hr.system.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 角色服务实现。
 *
 * @author Enterprise HR Team
 */
@Service
public class SysRoleServiceImpl implements SysRoleService {

    /** 角色编码前缀，统一编码风格，便于权限注解引用 */
    private static final String ROLE_CODE_PREFIX = "ROLE_";

    private final SysRoleMapper roleMapper;

    public SysRoleServiceImpl(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public PageResult<SysRoleVO> list(PageQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .orderByAsc(SysRole::getSortNo)
                .orderByAsc(SysRole::getId);
        List<SysRoleVO> all = roleMapper.selectList(wrapper).stream().map(this::toVO).toList();
        if (query == null) {
            return PageResult.list(all);
        }
        // 角色集合规模极小，统一走内存分页：既保证与数据库分页语义一致，
        // 也避免"前端下拉框拿不到完整列表"的两难
        return PageResult.of(all, query.safeCurrent(), query.safeSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO create(SysRoleSaveReq req) {
        String roleCode = normalizeRoleCode(req.getRoleCode());
        // 同 SysUserServiceImpl：role_code 上的唯一键不随逻辑删除释放，
        // 必须连逻辑已删除的记录一起查，才能给出可执行的提示
        long occupied = roleMapper.countByRoleCodeIncludeDeleted(roleCode);
        if (occupied > 0) {
            long alive = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, roleCode));
            if (alive > 0) {
                throw new BizException(ErrorCode.ROLE_CODE_DUPLICATED);
            }
            throw new BizException(ErrorCode.ROLE_CODE_DUPLICATED,
                    "角色编码「" + roleCode + "」曾被删除，编码仍被占用不可复用，请更换编码");
        }

        SysRole entity = new SysRole();
        entity.setRoleCode(roleCode);
        entity.setRoleName(req.getRoleName().trim());
        entity.setDescription(req.getDescription());
        entity.setDataScope(resolveDataScope(req.getDataScope()));
        entity.setDataScopeDepts(req.getDataScopeDepts());
        // 通过接口新建的一律是自定义角色；内置角色只能由初始化脚本产生
        entity.setRoleType(BizConst.ROLE_CUSTOM);
        entity.setSortNo(nextSortNo());
        entity.setStatus(req.getStatus() == null ? BizConst.STATUS_ENABLED : req.getStatus());
        roleMapper.insert(entity);
        return OpResultVO.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO update(Long id, SysRoleSaveReq req) {
        SysRole existed = id == null ? null : roleMapper.selectById(id);
        if (existed == null) {
            throw BizException.notFound("角色不存在");
        }

        // 内置角色的编码是权限注解（@RequireRoles）的引用目标，改动会静默失效，
        // 因此显式拒绝而不是"忽略该字段"——静默忽略让调用方以为改成功了
        if (existed.getRoleType() != null && existed.getRoleType() == BizConst.ROLE_BUILTIN
                && StringUtils.hasText(req.getRoleCode())
                && !existed.getRoleCode().equals(normalizeRoleCode(req.getRoleCode()))) {
            throw new BizException(ErrorCode.BUILTIN_ROLE_IMMUTABLE);
        }

        SysRole patch = new SysRole();
        patch.setId(id);
        if (StringUtils.hasText(req.getRoleName())) {
            patch.setRoleName(req.getRoleName().trim());
        }
        if (req.getDataScope() != null) {
            patch.setDataScope(resolveDataScope(req.getDataScope()));
        }
        if (req.getDataScopeDepts() != null) {
            patch.setDataScopeDepts(req.getDataScopeDepts());
        }
        if (req.getDescription() != null) {
            patch.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            patch.setStatus(req.getStatus());
        }
        roleMapper.updateById(patch);
        return OpResultVO.ok();
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 归一化角色编码：统一大写并补全 {@code ROLE_} 前缀。
     * 前端填写 {@code audit} 与 {@code ROLE_AUDIT} 应当等价，避免同一权限出现两种写法。
     */
    private String normalizeRoleCode(String raw) {
        String code = raw.trim().toUpperCase();
        return code.startsWith(ROLE_CODE_PREFIX) ? code : ROLE_CODE_PREFIX + code;
    }

    private int resolveDataScope(Integer dataScope) {
        if (dataScope == null) {
            // 缺省最保守：仅本人。宁可给少了让管理员显式放大，也不要默认放大导致越权
            return DataScopeType.SELF.getValue();
        }
        // DataScopeType.of() 对未知取值会兜底为最保守项，这里必须显式拒绝非法值，
        // 否则「前端传 9」会被静默落库成 4，管理员以为设成了自定义范围
        boolean valid = Arrays.stream(DataScopeType.values())
                .anyMatch(type -> type.getValue() == dataScope);
        if (!valid) {
            throw BizException.badRequest("数据范围仅支持 1~5（全部/本部门/本部门及下级/仅本人/自定义）");
        }
        return dataScope;
    }

    /** 排序号取当前最大值 +1，让新增角色排在末尾而不是随机插入 */
    private int nextSortNo() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()).stream()
                .map(SysRole::getSortNo)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setDataScope(role.getDataScope());
        vo.setDataScopeDepts(role.getDataScopeDepts());
        vo.setRoleType(role.getRoleType());
        vo.setSortNo(role.getSortNo());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        return vo;
    }
}
