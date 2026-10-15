package com.enterprise.hr.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.auth.dto.LoginReq;
import com.enterprise.hr.auth.dto.vo.LoginUserVO;
import com.enterprise.hr.auth.dto.vo.LoginVO;
import com.enterprise.hr.auth.service.AuthService;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.JwtUtil;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.config.EhrProperties;
import com.enterprise.hr.employee.entity.SysRole;
import com.enterprise.hr.employee.entity.SysUser;
import com.enterprise.hr.employee.entity.SysUserRole;
import com.enterprise.hr.system.entity.SysLoginLog;
import com.enterprise.hr.system.mapper.SysLoginLogMapper;
import com.enterprise.hr.system.mapper.SysRoleMapper;
import com.enterprise.hr.system.mapper.SysUserMapper;
import com.enterprise.hr.system.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务实现。
 *
 * <p>安全设计要点（BR-09-01 / BR-09-03）：
 * <ul>
 *     <li><b>失败信息不区分账号</b>：账号不存在与口令错误返回同一提示，避免账号枚举</li>
 *     <li><b>连续失败锁定</b>：达到 {@code ehr.security.max-login-fail} 后锁定
 *         {@code lock-minutes} 分钟；到期后由下次登录自动解锁</li>
 *     <li><b>成功登录清零</b>：重置失败计数与锁定状态，避免"历史失败"累积误锁</li>
 *     <li><b>全量留痕</b>：成功与失败都写 {@code sys_login_log}，供 F-09-06 日志审计</li>
 *     <li><b>口令不可逆</b>：BCrypt 校验，日志与响应中永不出现口令字段</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 登录成功 */
    private static final int LOGIN_SUCCESS = 1;
    /** 登录失败 */
    private static final int LOGIN_FAIL = 0;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EhrProperties properties;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysUserRoleMapper userRoleMapper,
                           SysRoleMapper roleMapper,
                           SysLoginLogMapper loginLogMapper,
                           JwtUtil jwtUtil,
                           BCryptPasswordEncoder passwordEncoder,
                           EhrProperties properties) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.loginLogMapper = loginLogMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginReq req, String loginIp, String browser, String os) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username).last("LIMIT 1"));

        // 账号不存在：与口令错误返回同一提示，避免账号枚举
        if (user == null) {
            writeLoginLog(null, username, loginIp, browser, os, LOGIN_FAIL, "账号或密码错误");
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        // 账号停用
        if (user.getStatus() != null && user.getStatus() == BizConst.USER_DISABLED) {
            writeLoginLog(user.getId(), username, loginIp, browser, os, LOGIN_FAIL, "账号已停用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 账号锁定（未到期前拒绝；到期后自动解锁继续校验口令）
        if (user.getStatus() != null && user.getStatus() == BizConst.USER_LOCKED
                && user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            writeLoginLog(user.getId(), username, loginIp, browser, os, LOGIN_FAIL, "账号锁定中");
            throw new BizException(ErrorCode.ACCOUNT_LOCKED,
                    "账号已锁定，请于 " + user.getLockUntil() + " 后重试或联系管理员");
        }

        // 口令校验
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            int maxFail = properties.getSecurity().getMaxLoginFail();
            SysUser patch = new SysUser();
            patch.setId(user.getId());
            patch.setLoginFailCount(failCount);
            String message;
            if (failCount >= maxFail) {
                patch.setStatus(BizConst.USER_LOCKED);
                patch.setLockUntil(LocalDateTime.now().plusMinutes(properties.getSecurity().getLockMinutes()));
                message = "账号或密码错误，连续失败 " + failCount + " 次，账号已锁定 "
                        + properties.getSecurity().getLockMinutes() + " 分钟";
            } else {
                message = "账号或密码错误，还可尝试 " + (maxFail - failCount) + " 次";
            }
            // 用 updateById 局部更新：只传需要改的列（MyBatis-Plus 默认忽略 null 字段）
            userMapper.updateById(patch);
            writeLoginLog(user.getId(), username, loginIp, browser, os, LOGIN_FAIL, message);
            throw new BizException(ErrorCode.BAD_CREDENTIALS, message);
        }

        // 登录成功：清零失败计数、解除锁定、记录登录时间与 IP
        LocalDateTime loginTime = LocalDateTime.now();
        // 说明：lockUntil 必须显式置为 NULL，而 updateById 会忽略 null 字段，
        // 因此这里改用 UpdateWrapper 精确控制 SET 子句。
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getStatus, BizConst.USER_NORMAL)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getLastLoginTime, loginTime)
                .set(SysUser::getLastLoginIp, loginIp));

        writeLoginLog(user.getId(), username, loginIp, browser, os, LOGIN_SUCCESS, "登录成功");

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginVO.builder()
                .token(token)
                .user(buildUserVO(user))
                .build();
    }

    @Override
    public LoginUserVO currentUser() {
        LoginUser context = UserContext.require();
        SysUser user = userMapper.selectById(context.getUserId());
        if (user == null) {
            // 令牌有效但用户已被删除：按未认证处理，让前端回到登录页
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LoginUserVO vo = buildUserVO(user);
        // 角色以本次请求实时装载的结果为准（context 中的角色来自 sys_user_role）
        vo.setRoles(new ArrayList<>(context.getRoles()));
        return vo;
    }

    @Override
    public void logout() {
        LoginUser context = UserContext.get();
        if (context != null) {
            log.info("用户退出登录: userId={} username={}", context.getUserId(), context.getUsername());
        }
    }

    // ------------------------------------------------------------------ 内部

    private LoginUserVO buildUserVO(SysUser user) {
        return LoginUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername())
                .employeeId(user.getEmployeeId())
                .deptId(user.getDeptId())
                .roles(listRoleCodes(user.getId()))
                .avatar(user.getAvatar())
                .build();
    }

    /** 查询用户已启用角色的编码集合（前端路由守卫据此过滤菜单） */
    private List<String> listRoleCodes(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct().toList();
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds)).stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == BizConst.STATUS_ENABLED)
                .map(SysRole::getRoleCode)
                .distinct()
                .toList();
    }

    private void writeLoginLog(Long userId, String username, String ip, String browser, String os,
                               int status, String message) {
        try {
            SysLoginLog entity = new SysLoginLog();
            entity.setUserId(userId);
            entity.setUsername(username);
            entity.setLoginIp(ip);
            entity.setBrowser(browser);
            entity.setOs(os);
            entity.setLoginStatus(status);
            entity.setMessage(message);
            entity.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(entity);
        } catch (Exception e) {
            // 审计日志失败不得影响登录主流程
            log.error("登录日志写入失败: username={}", username, e);
        }
    }
}
