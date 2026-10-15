package com.enterprise.hr.config;

import com.enterprise.hr.common.security.AuthInterceptor;
import com.enterprise.hr.common.security.JwtUtil;
import com.enterprise.hr.common.security.LoginUserLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册认证拦截器 + 跨域策略。
 *
 * <p>拦截器覆盖 {@code /**}，只把「框架自身资源」与「登录入口」排除在外；
 * 业务接口一律默认需要认证（fail-close），新增控制器无需额外配置即受保护。
 *
 * <p>排除清单与 {@code ehr.security.permit-urls} 区别：
 * 这里是"根本不进入拦截器"（如 swagger 静态资源），
 * 而 permit-urls 是"进入拦截器但放行"（如登录接口，便于统一打日志）。
 *
 * @author Enterprise HR Team
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final LoginUserLoader loginUserLoader;
    private final EhrProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtil, loginUserLoader, properties))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 接口文档与静态资源：不涉及业务数据，且 swagger 页面无法携带令牌
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico",
                        // 容器内部转发（异常、错误页），避免二次鉴权把原始错误码覆盖成 401
                        "/error"
                );
    }

    /**
     * 跨域策略。
     *
     * <p>前端开发态通过 Vite 代理（同源）访问，本配置主要服务于
     * "前端直连后端端口"的联调场景与后续前后端分域部署。
     * 使用 {@code allowedOriginPatterns} 而非 {@code allowedOrigins}：
     * 后者与 {@code allowCredentials=true} 组合在 Spring 6 下会被拒绝。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
