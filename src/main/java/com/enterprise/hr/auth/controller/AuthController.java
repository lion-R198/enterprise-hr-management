package com.enterprise.hr.auth.controller;

import com.enterprise.hr.auth.dto.LoginReq;
import com.enterprise.hr.auth.dto.vo.LoginUserVO;
import com.enterprise.hr.auth.dto.vo.LoginVO;
import com.enterprise.hr.auth.service.AuthService;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证接口。
 *
 * <p>{@code /auth/login} 是唯一免鉴权接口（见 {@code ehr.security.permit-urls}），
 * 其余接口由 {@code AuthInterceptor} 统一拦截。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "认证", description = "登录 / 会话信息 / 退出")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 账号密码登录，返回 JWT 令牌与用户信息 */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        return R.ok(authService.login(req, ip,
                request.getHeader("User-Agent"), resolveOs(request.getHeader("User-Agent"))));
    }

    /** 当前登录用户信息（页面刷新后重建会话） */
    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public R<LoginUserVO> me() {
        return R.ok(authService.currentUser());
    }

    /** 退出登录（服务端仅留痕，令牌由前端清除） */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 从 User-Agent 粗略识别操作系统，仅用于登录日志展示。
     * 不引入第三方 UA 解析库：这里只需要可读的粗略值，多一个依赖不划算。
     */
    private String resolveOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "未知";
    }
}
