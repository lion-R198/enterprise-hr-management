package com.enterprise.hr.auth.service;

import com.enterprise.hr.auth.dto.LoginReq;
import com.enterprise.hr.auth.dto.vo.LoginUserVO;
import com.enterprise.hr.auth.dto.vo.LoginVO;

/**
 * 认证服务：登录、会话信息、退出。
 *
 * @author Enterprise HR Team
 */
public interface AuthService {

    /**
     * 账号密码登录。
     *
     * <p>校验链路：账号存在 → 账号未停用 → 账号未锁定 → 口令正确。
     * 任一环节失败都会写入 {@code sys_login_log}（成功/失败都留痕），
     * 连续失败达到阈值自动锁定账号。
     *
     * @param req    登录请求
     * @param loginIp 客户端 IP（审计用）
     * @param browser 浏览器标识（审计用）
     * @param os      操作系统标识（审计用）
     * @return 令牌与用户信息
     */
    LoginVO login(LoginReq req, String loginIp, String browser, String os);

    /**
     * 取当前登录用户信息（页面刷新后由前端路由守卫调用，用于重建会话）。
     *
     * @return 当前用户信息
     */
    LoginUserVO currentUser();

    /**
     * 退出登录。
     *
     * <p>令牌是无状态的，服务端不维护会话，因此登出只做审计留痕；
     * 真正的失效由前端清除本地令牌完成。若后续需要"强制下线"，
     * 应在 {@code AuthInterceptor} 中引入令牌黑名单（Redis）。
     */
    void logout();
}
