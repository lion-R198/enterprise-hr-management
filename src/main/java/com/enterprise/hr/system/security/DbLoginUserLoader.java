package com.enterprise.hr.system.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.DataScopeType;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.LoginUserLoader;
import com.enterprise.hr.employee.entity.SysRole;
import com.enterprise.hr.employee.entity.SysUser;
import com.enterprise.hr.employee.entity.SysUserRole;
import com.enterprise.hr.system.mapper.SysRoleMapper;
import com.enterprise.hr.system.mapper.SysUserMapper;
import com.enterprise.hr.system.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于数据库的登录用户装载器。
 *
 * <p>每次请求按 {@code sys_user → sys_user_role → sys_role} 三级查询重建用户上下文。
 * 这是"角色即时生效"的代价（3 次轻量索引查询），换来的是安全性：
 * 令牌期内停用账号、调整角色、变更部门都会立刻反映到权限判定上。
 *
 * <p>多角色数据范围合并规则：<b>取最宽</b>（{@code data_scope} 数值最小者）。
 * 例如同时持有「部门主管(3)」与「普通员工(4)」时按 3 生效，
 * 否则员工角色会把主管的可见范围意外收窄。
 *
 * @author Enterprise HR Team
 */
@Component
public class DbLoginUserLoader implements LoginUserLoader {

    private static final Logger log = LoggerFactory.getLogger(DbLoginUserLoader.class);

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public DbLoginUserLoader(SysUserMapper userMapper,
                             SysUserRoleMapper userRoleMapper,
                             SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public LoginUser load(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            log.debug("令牌对应用户不存在或已删除: userId={}", userId);
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == BizConst.USER_DISABLED) {
            log.debug("账号已停用: userId={}", userId);
            return null;
        }
        // 锁定到期自动解锁：lock_until 已过则视为正常，避免"永久锁定"无人解
        if (user.getStatus() != null && user.getStatus() == BizConst.USER_LOCKED) {
            LocalDateTime lockUntil = user.getLockUntil();
            if (lockUntil == null || lockUntil.isAfter(LocalDateTime.now())) {
                log.debug("账号锁定中: userId={} lockUntil={}", userId, lockUntil);
                return null;
            }
        }

        List<SysRole> roles = listEnabledRoles(userId);
        Set<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .employeeId(user.getEmployeeId())
                .deptId(user.getDeptId())
                .avatar(user.getAvatar())
                .roles(roleCodes)
                .dataScope(resolveDataScope(roles))
                .scopeDeptIds(resolveScopeDepts(roles))
                .build();
    }

    /** 查询用户已启用的角色（按 sort_no 排序，保证 getRoles() 输出顺序稳定） */
    public List<SysRole> listEnabledRoles(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds));
        return roles.stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == BizConst.STATUS_ENABLED)
                .sorted(Comparator.comparing(r -> r.getSortNo() == null ? 0 : r.getSortNo()))
                .toList();
    }

    private DataScopeType resolveDataScope(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return DataScopeType.SELF;
        }
        int widest = roles.stream()
                .map(SysRole::getDataScope)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(DataScopeType.SELF.getValue());
        return DataScopeType.of(widest);
    }

    private List<Long> resolveScopeDepts(List<SysRole> roles) {
        List<Long> ids = new ArrayList<>();
        for (SysRole role : roles) {
            if (role.getDataScope() != null && role.getDataScope() == DataScopeType.CUSTOM.getValue()
                    && StringUtils.hasText(role.getDataScopeDepts())) {
                for (String part : role.getDataScopeDepts().split(",")) {
                    String v = part.trim();
                    if (v.isEmpty()) {
                        continue;
                    }
                    try {
                        ids.add(Long.valueOf(v));
                    } catch (NumberFormatException ignored) {
                        log.warn("角色自定义数据范围含非法部门ID: roleId={} value={}", role.getId(), v);
                    }
                }
            }
        }
        return ids;
    }
}
