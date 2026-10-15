package com.enterprise.hr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 口令哈希配置。
 *
 * <p>只使用 {@code spring-security-crypto} 的 BCrypt 实现，<b>不引入完整的 Spring Security</b>
 * ——后者会启用整条安全过滤链，与本项目自研的 JWT 拦截器方案冲突（表现为所有接口 401）。
 *
 * <p>BCrypt 强度 10：单次校验约 50~80ms，足以抵抗离线爆破，
 * 又不会让登录接口在高并发下成为瓶颈。数据库 {@code sys_user.password} 已按 BCrypt 存储（60 字符）。
 *
 * @author Enterprise HR Team
 */
@Configuration
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
