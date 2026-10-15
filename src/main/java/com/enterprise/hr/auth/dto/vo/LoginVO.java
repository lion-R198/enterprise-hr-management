package com.enterprise.hr.auth.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应：令牌 + 用户信息。
 *
 * <p>与前端 {@code store/modules/user.js#login} 契约一致：
 * {@code data.token} 写入本地存储，{@code data.user} 写入会话状态。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT 令牌，前端以 {@code Authorization: Bearer <token>} 回传 */
    private String token;
    /** 登录用户信息 */
    private LoginUserVO user;
}
