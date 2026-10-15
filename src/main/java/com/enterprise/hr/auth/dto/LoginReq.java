package com.enterprise.hr.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求。
 *
 * @author Enterprise HR Team
 */
@Data
public class LoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录账号 */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /** 登录密码（明文传输，生产环境必须走 HTTPS） */
    @NotBlank(message = "登录密码不能为空")
    private String password;
}
