package com.enterprise.hr.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 获取工具（审计日志用）。
 *
 * <p>按代理链依次读取，取第一个非 unknown 的地址。
 * 注意：这些头部可被客户端伪造，仅用于审计留痕，<b>不可作为安全校验依据</b>。
 *
 * @author Enterprise HR Team
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_V4 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_V6 = "127.0.0.1";

    private static final String[] HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtil() {
    }

    /** 取当前请求的客户端 IP；非 Web 线程返回 null */
    public static String getClientIp() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : getClientIp(request);
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value.trim())) {
                // X-Forwarded-For 形如 "client, proxy1, proxy2"，第一个才是真实客户端
                int comma = value.indexOf(',');
                String ip = (comma > 0 ? value.substring(0, comma) : value).trim();
                if (StringUtils.hasText(ip)) {
                    return normalize(ip);
                }
            }
        }
        return normalize(request.getRemoteAddr());
    }

    /** 当前请求对象；非 Web 线程返回 null */
    public static HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private static String normalize(String ip) {
        if (ip == null) {
            return null;
        }
        return LOCALHOST_V6.equals(ip) ? LOCALHOST_V4 : ip;
    }
}
