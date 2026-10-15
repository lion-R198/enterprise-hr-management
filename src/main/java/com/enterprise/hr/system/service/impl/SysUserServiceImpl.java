package com.enterprise.hr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.config.EhrProperties;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.SysRole;
import com.enterprise.hr.employee.entity.SysUser;
import com.enterprise.hr.employee.entity.SysUserRole;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.system.dto.SysUserCreateReq;
import com.enterprise.hr.system.dto.SysUserQuery;
import com.enterprise.hr.system.dto.SysUserUpdateReq;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import com.enterprise.hr.system.dto.vo.SysUserVO;
import com.enterprise.hr.system.mapper.SysRoleMapper;
import com.enterprise.hr.system.mapper.SysUserMapper;
import com.enterprise.hr.system.mapper.SysUserRoleMapper;
import com.enterprise.hr.system.service.SysUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现。
 *
 * <p>关键设计：
 * <ul>
 *     <li><b>批量装配展示字段</b>：员工名/部门名/角色名都用一次 {@code IN} 查询建映射，
 *         再在内存里补齐，避免一页 10 条发出 30 次单条查询（N+1）</li>
 *     <li><b>口令只进不出</b>：BCrypt 哈希写入，任何响应都不含口令字段</li>
 *     <li><b>管理员保护</b>（BR-09-02）：不可停用自身管理员账号，也不可让系统失去最后一个启用管理员</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    /** BR-09-01 密码最小长度 */
    private static final int MIN_PASSWORD_LENGTH = 8;
    /** 角色名连接符 */
    private static final String ROLE_NAME_SEPARATOR = "、";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EhrProperties properties;

    public SysUserServiceImpl(SysUserMapper userMapper,
                              SysUserRoleMapper userRoleMapper,
                              SysRoleMapper roleMapper,
                              EmployeeMapper employeeMapper,
                              DeptMapper deptMapper,
                              BCryptPasswordEncoder passwordEncoder,
                              EhrProperties properties) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.employeeMapper = employeeMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ 查询

    @Override
    public PageResult<SysUserVO> page(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysUser::getId);

        IPage<SysUser> page = userMapper.selectPage(query.toPage(), wrapper);
        List<SysUser> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        Map<Long, String> employeeNames = loadEmployeeNames(records);
        Map<Long, String> deptNames = loadDeptNames(records);
        Map<Long, List<SysRole>> rolesByUser = loadRoles(records.stream().map(SysUser::getId).toList());
        Map<String, String> roleNameByCode = loadRoleNameMap();

        List<SysUserVO> list = new ArrayList<>(records.size());
        for (SysUser user : records) {
            SysUserVO vo = toVO(user);
            vo.setEmployeeName(user.getEmployeeId() == null ? null : employeeNames.get(user.getEmployeeId()));
            vo.setDeptName(user.getDeptId() == null ? null : deptNames.get(user.getDeptId()));
            List<SysRole> roles = rolesByUser.getOrDefault(user.getId(), List.of());
            List<String> codes = roles.stream().map(SysRole::getRoleCode).toList();
            vo.setRoles(codes);
            vo.setRoleNames(codes.stream()
                    .map(code -> roleNameByCode.getOrDefault(code, code))
                    .collect(Collectors.joining(ROLE_NAME_SEPARATOR)));
            list.add(vo);
        }
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ------------------------------------------------------------------ 新增

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO create(SysUserCreateReq req) {
        String username = req.getUsername().trim();
        // 先查「含逻辑已删除」的总数：username 上有唯一键，逻辑删除不释放该值。
        // 若只查未删除记录，账号曾被删除时这里会认为可用，INSERT 才被唯一键拦下，
        // 用户最终看到的是语义模糊的"数据已存在"，无从判断该怎么处理。
        long occupied = userMapper.countByUsernameIncludeDeleted(username);
        if (occupied > 0) {
            long alive = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
            if (alive > 0) {
                throw new BizException(ErrorCode.USERNAME_DUPLICATED);
            }
            throw new BizException(ErrorCode.USERNAME_DUPLICATED,
                    "登录账号「" + username + "」曾被删除，账号名仍被占用不可复用，请更换账号名或联系管理员恢复该账号");
        }

        String rawPassword = StringUtils.hasText(req.getPassword())
                ? req.getPassword()
                : properties.getSecurity().getInitPassword();
        checkPasswordStrength(rawPassword);

        SysUser entity = new SysUser();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setRealName(req.getRealName());
        entity.setEmployeeId(req.getEmployeeId());
        entity.setDeptId(req.getDeptId());
        entity.setPhone(req.getPhone());
        entity.setEmail(req.getEmail());
        entity.setStatus(BizConst.USER_NORMAL);
        entity.setLoginFailCount(0);
        userMapper.insert(entity);

        assignRoles(entity.getId(), normalizeRoles(req.getRoles()));
        return OpResultVO.ok(entity.getId());
    }

    // ------------------------------------------------------------------ 修改

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO update(Long id, SysUserUpdateReq req) {
        requireUser(id);
        boolean isAdminAccount = hasRole(id, RoleCode.ADMIN);

        // ---- 账号状态（前端「停用/启用」按钮只传 status 一个字段） ----
        if (req.getStatus() != null) {
            int status = req.getStatus();
            if (status != BizConst.USER_NORMAL && status != BizConst.USER_DISABLED) {
                throw BizException.badRequest("账号状态仅支持 1（正常）或 0（停用）");
            }
            if (status == BizConst.USER_DISABLED && isAdminAccount) {
                // BR-09-02：不可停用自身的管理员账号
                Long currentUserId = UserContext.currentUserId();
                if (id.equals(currentUserId)) {
                    throw new BizException(ErrorCode.ADMIN_PROTECTED, "不可停用自身的管理员账号（BR-09-02）");
                }
                // 兜底：系统必须保留至少一个启用状态的管理员账号，否则无人能再进系统
                if (countEnabledAdmins() <= 1) {
                    throw new BizException(ErrorCode.ADMIN_PROTECTED,
                            "系统必须保留至少一个启用状态的管理员账号（BR-09-02）");
                }
            }
            LambdaUpdateWrapper<SysUser> statusUpdate = Wrappers.<SysUser>lambdaUpdate()
                    .eq(SysUser::getId, id)
                    .set(SysUser::getStatus, status);
            if (status == BizConst.USER_NORMAL) {
                // 启用时顺带清空失败计数与锁定截止时间：
                // 否则会出现「管理员已启用账号，但用户因历史锁定仍登不进去」的诡异现象
                statusUpdate.set(SysUser::getLoginFailCount, 0).set(SysUser::getLockUntil, null);
            }
            userMapper.update(null, statusUpdate);
        }

        // ---- 基础资料（局部更新，未传字段保持原值） ----
        SysUser patch = new SysUser();
        patch.setId(id);
        boolean needUpdate = false;
        if (StringUtils.hasText(req.getRealName())) {
            patch.setRealName(req.getRealName());
            needUpdate = true;
        }
        if (req.getEmployeeId() != null) {
            patch.setEmployeeId(req.getEmployeeId());
            needUpdate = true;
        }
        if (req.getDeptId() != null) {
            patch.setDeptId(req.getDeptId());
            needUpdate = true;
        }
        if (req.getPhone() != null) {
            patch.setPhone(req.getPhone());
            needUpdate = true;
        }
        if (req.getEmail() != null) {
            patch.setEmail(req.getEmail());
            needUpdate = true;
        }
        if (needUpdate) {
            userMapper.updateById(patch);
        }

        // ---- 角色（全量覆盖语义） ----
        if (req.getRoles() != null) {
            assignRoles(id, normalizeRoles(req.getRoles()));
        }
        return OpResultVO.ok();
    }

    // ------------------------------------------------------------------ 重置密码 / 删除

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO resetPassword(Long id) {
        requireUser(id);
        String initPassword = properties.getSecurity().getInitPassword();
        // 重置密码同时解锁账号：忘记密码导致的锁定应由管理员重置动作一次性解决
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(initPassword))
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getStatus, BizConst.USER_NORMAL)
                .set(SysUser::getLockUntil, null));
        return OpResultVO.withInitPassword(initPassword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO delete(Long id) {
        requireUser(id);
        // BR-09-02：管理员账号不可删除
        if (hasRole(id, RoleCode.ADMIN)) {
            throw new BizException(ErrorCode.ADMIN_PROTECTED, "管理员账号不可删除（BR-09-02）");
        }
        // 逻辑删除（@TableLogic）：账号立即失效，历史登录/操作日志仍可通过 user_id 追溯
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        return OpResultVO.ok();
    }

    // ------------------------------------------------------------------ 内部

    private SysUser requireUser(Long id) {
        SysUser user = id == null ? null : userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    private void checkPasswordStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BizException(ErrorCode.PASSWORD_WEAK);
        }
    }

    /** 未指定角色时给最小权限角色，避免"无角色"账号绕过角色校验 */
    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(RoleCode.EMPLOYEE);
        }
        return roles.stream().filter(StringUtils::hasText).distinct().toList();
    }

    /** 全量覆盖用户角色：先删后插，语义清晰且不受前端传参顺序影响 */
    private void assignRoles(Long userId, List<String> roleCodes) {
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().in(SysRole::getRoleCode, roleCodes));
        Set<String> found = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missing = roleCodes.stream().filter(code -> !found.contains(code)).toList();
        if (!missing.isEmpty()) {
            throw BizException.badRequest("角色不存在：" + String.join("、", missing));
        }
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        for (SysRole role : roles) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            userRoleMapper.insert(relation);
        }
    }

    private boolean hasRole(Long userId, String roleCode) {
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return false;
        }
        return roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getRoleCode, roleCode)) > 0;
    }

    /** 统计启用状态且持有管理员角色的账号数，用于"最后一个管理员"保护 */
    private long countEnabledAdmins() {
        List<Long> adminRoleIds = roleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, RoleCode.ADMIN))
                .stream().map(SysRole::getId).toList();
        if (adminRoleIds.isEmpty()) {
            return 0;
        }
        List<Long> userIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getRoleId, adminRoleIds))
                .stream().map(SysUserRole::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return 0;
        }
        return userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getStatus, BizConst.USER_NORMAL));
    }

    private Map<Long, String> loadEmployeeNames(List<SysUser> records) {
        return loadNames(records.stream().map(SysUser::getEmployeeId).filter(java.util.Objects::nonNull).toList(),
                ids -> employeeMapper.selectList(Wrappers.<Employee>lambdaQuery().in(Employee::getId, ids)),
                Employee::getId, Employee::getName);
    }

    private Map<Long, String> loadDeptNames(List<SysUser> records) {
        return loadNames(records.stream().map(SysUser::getDeptId).filter(java.util.Objects::nonNull).toList(),
                ids -> deptMapper.selectList(Wrappers.<Dept>lambdaQuery().in(Dept::getId, ids)),
                Dept::getId, Dept::getName);
    }

    private <T> Map<Long, String> loadNames(List<Long> ids,
                                            Function<Collection<Long>, List<T>> loader,
                                            Function<T, Long> idGetter,
                                            Function<T, String> nameGetter) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return loader.apply(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(idGetter, nameGetter, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, List<SysRole>> loadRoles(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> relations = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, userIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysRole> roleById = roleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery().in(SysRole::getId,
                                relations.stream().map(SysUserRole::getRoleId).distinct().toList()))
                .stream().collect(Collectors.toMap(SysRole::getId, Function.identity(), (a, b) -> a));

        Map<Long, List<SysRole>> result = new LinkedHashMap<>();
        for (SysUserRole relation : relations) {
            SysRole role = roleById.get(relation.getRoleId());
            if (role != null) {
                result.computeIfAbsent(relation.getUserId(), k -> new ArrayList<>()).add(role);
            }
        }
        return result;
    }

    private Map<String, String> loadRoleNameMap() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getRoleName, (a, b) -> a, LinkedHashMap::new));
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmployeeId(user.getEmployeeId());
        vo.setDeptId(user.getDeptId());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        return vo;
    }
}
