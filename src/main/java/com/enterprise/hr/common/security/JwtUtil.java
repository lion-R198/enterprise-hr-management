package com.enterprise.hr.common.security;

import com.enterprise.hr.config.EhrProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 令牌工具：签发与校验（HS256 对称签名）。
 *
 * <p>设计取舍：
 * <ul>
 *     <li>令牌只携带 {@code userId} 与 {@code username}，<b>不携带角色</b>。
 *         角色在每次请求时查库重建（{@link AuthInterceptor}），
 *         保证"停用账号/调整角色"即时生效，避免令牌有效期内一直持旧权限。</li>
 *     <li>无状态：服务端不保存会话，便于水平扩容；退出登录由前端清令牌完成，
 *         服务端通过 {@code sys_login_log} 留痕。</li>
 *     <li>签名密钥来自配置 {@code ehr.jwt.secret}，生产环境必须用环境变量覆盖。</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 自定义声明：登录账号 */
    public static final String CLAIM_USERNAME = "username";

    /** 请求头名称 */
    public static final String HEADER = "Authorization";
    /** 令牌前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    private final EhrProperties properties;
    private final SecretKey secretKey;

    public JwtUtil(EhrProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // HS256 要求密钥不少于 256 位；配置过短直接失败，避免"看起来能跑但不安全"
            throw new IllegalStateException("ehr.jwt.secret 长度不足 32 字节，HS256 签名不安全，请更换更长的密钥");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发令牌。
     *
     * @param userId   用户 ID
     * @param username 登录账号
     * @return 紧凑格式的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(properties.getJwt().getExpireMinutes() * 60L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验令牌。
     *
     * @param token 不含 {@code Bearer } 前缀的令牌串
     * @return 声明集合；令牌非法/过期/签名不匹配时返回 {@code null}（由调用方转 401）
     */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌异常属预期分支（前端过期、伪造探测），记 debug 即可，避免刷屏
            log.debug("令牌校验失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从令牌中取出用户 ID；无法解析时返回 null */
    public Long extractUserId(String token) {
        Claims claims = parse(token);
        if (claims == null || claims.getSubject() == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            log.debug("令牌 subject 非数字: {}", claims.getSubject());
            return null;
        }
    }

    /** 剥掉 {@code Bearer } 前缀 */
    public static String stripBearer(String header) {
        if (header == null) {
            return null;
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return value.substring(BEARER_PREFIX.length()).trim();
        }
        return value;
    }

    /** 令牌有效期（秒），用于登录响应回传 */
    public long getExpireSeconds() {
        return properties.getJwt().getExpireMinutes() * 60L;
    }
}
