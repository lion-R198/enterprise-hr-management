package com.enterprise.hr.common.util;

import org.springframework.util.StringUtils;

/**
 * 敏感信息脱敏工具（NF-08：数据层脱敏，前端拿不到明文）。
 *
 * <p>脱敏必须在服务端完成：只有"查询结果里本来就没有明文"，才能真正避免
 * 通过浏览器调试工具、接口重放、日志导出等途径获取敏感数据。
 *
 * @author Enterprise HR Team
 */
public final class MaskUtil {

    /** 星号占位 */
    private static final String STAR = "*";

    private MaskUtil() {
    }

    /**
     * 身份证脱敏：保留前 6 位（地址码）与后 2 位，中间以 10 个星号填充。
     *
     * <p>与前端 {@code employee/index.vue} 标注的「身份证（脱敏）」展示规则一致：
     * {@code 110101**********23}。保留后 2 位便于本人核对，泄露风险极低。
     *
     * @param idCard 明文身份证号；长度不足时返回原值（避免把短字符串抹成星号造成误解）
     */
    public static String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return idCard;
        }
        String value = idCard.trim();
        if (value.length() < 10) {
            return value;
        }
        return value.substring(0, 6) + STAR.repeat(10) + value.substring(value.length() - 2);
    }

    /**
     * 手机号脱敏：{@code 138****8000}。
     * 保留前 3 位号段与后 4 位，中间 4 位打码。
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + STAR.repeat(value.length() - 7) + value.substring(value.length() - 4);
    }

    /**
     * 银行卡脱敏：保留前 4 位（卡组织标识）与后 4 位。
     * 已含星号的"半脱敏"值原样返回，避免二次处理把星号也打掉。
     */
    public static String maskBankAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return account;
        }
        String value = account.trim();
        if (value.contains(STAR) || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + STAR.repeat(value.length() - 8) + value.substring(value.length() - 4);
    }

    /**
     * 姓名脱敏：保留姓氏，其余打码（{@code 张**}）。
     * 用于"员工薪资排行"等对外展示场景。
     */
    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return name;
        }
        String value = name.trim();
        if (value.length() <= 1) {
            return value;
        }
        return value.charAt(0) + STAR.repeat(value.length() - 1);
    }
}
