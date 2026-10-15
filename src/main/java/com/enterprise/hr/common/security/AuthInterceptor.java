package com.enterprise.hr.common.security;

import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.config.EhrProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 认证与授权拦截器（fail-close）。
 *
 * <p>处理顺序：
 * <ol>
 *     <li>放行免鉴权路径（登录接口、接口文档、错误页）与跨域预检 {@code OPTIONS}</li>
 *     <li>解析 {@code Authorization: Bearer <token>}；缺失或非法 → 401</li>
 *     <li>按令牌中的用户 ID <b>实时查库</b>重建 {@link LoginUser}；用户不存在或已停用 → 401 / 403</li>
 *     <li>写入 {@link UserContext}</li>
 *     <li>校验 {@link RequireRoles} 角色要求，不满足 → 403</li>
 * </ol>
 *
 * <p>"fail-close"含义：任何一步无法明确判定为合法，一律拒绝。宁可误拒也不放行，
 * 避免出现"配置写错导致接口裸奔"。
 *
 * <p>被排除的静态资源（前端构建产物、swagger 资源）由 {@code WebMvcConfig} 的
 * {@code excludePathPatterns} 处理，不进入本拦截器。
 *
 * @author Enterprise HR Team
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final LoginUserLoader loginUserLoader;
    private final EhrProperties properties;

    public AuthInterceptor(JwtUtil jwtUtil, LoginUserLoader loginUserLoader, EhrProperties properties) {
        this.jwtUtil = jwtUtil;
        this.loginUserLoader = loginUserLoader;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检直接放行：浏览器预检不带 Authorization 头
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (isPermitted(path)) {
            return true;
        }

        // 1) 令牌解析
        String token = JwtUtil.stripBearer(request.getHeader(JwtUtil.HEADER));
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            log.debug("未认证访问被拦截: {} {}", request.getMethod(), path);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 2) 实时重建用户（保证角色/状态变更即时生效）
        LoginUser loginUser = loginUserLoader.load(userId);
        if (loginUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (loginUser.getRoles().isEmpty()) {
            // 未分配角色的账号一律拒绝：避免"无角色 = 无限制"的实现陷阱
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号未分配任何角色，请联系系统管理员");
        }

        UserContext.set(loginUser);

        // 3) 接口级角色校验
        checkRoles(handler, loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 容器线程复用，必须清理 ThreadLocal，否则会串用户
        UserContext.clear();
    }

    // ------------------------------------------------------------------ 内部

    /** 是否命中免鉴权路径 */
    private boolean isPermitted(String path) {
        List<String> permitUrls = properties.getSecurity().getPermitUrls();
        if (permitUrls == null || permitUrls.isEmpty()) {
            return false;
        }
        return permitUrls.stream()
                .filter(org.springframework.util.StringUtils::hasText)
                .anyMatch(pattern -> PATH_MATCHER.match(pattern.trim(), path));
    }

    /**
     * 方法上的 {@link RequireRoles} 优先于类上的；两者都存在时合并语义为
     * "方法级要求"单独生效（允许在只读类上给写接口收窄权限）。
     */
    private void checkRoles(Object handler, LoginUser loginUser) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        RequireRoles require = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (require == null) {
            require = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        if (require == null || require.value().length == 0) {
            return;
        }
        // 系统管理员默认拥有全部接口权限，避免"加一个角色就要同步改所有注解"
        if (loginUser.hasAnyRole(RoleCode.ADMIN)) {
            return;
        }
        boolean pass = require.required()
                ? loginUser.hasAllRoles(require.value())
                : loginUser.hasAnyRole(require.value());
        if (!pass) {
            log.warn("越权访问被拦截: user={} roles={} 需要={} {}",
                    loginUser.getUsername(), loginUser.getRoles(),
                    Arrays.toString(require.value()), handlerMethod.getShortLogMessage());
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
