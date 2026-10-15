package com.enterprise.hr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务配置绑定（前缀 {@code ehr}）。
 *
 * <p>把阈值、密钥、免鉴权路径等从代码中抽到配置，便于按环境覆盖：
 * 生产环境用环境变量注入 {@code EHR_JWT_SECRET}、{@code DB_PASSWORD} 等，不落盘明文。
 *
 * @author Enterprise HR Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "ehr")
public class EhrProperties {

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Business business = new Business();

    @Data
    public static class Jwt {
        /** HS256 签名密钥，长度必须 ≥ 32 字节 */
        private String secret = "ehr-enterprise-hr-management-hs256-secret-key-2026-please-override";
        /** 有效期（分钟） */
        private long expireMinutes = 720L;
        /** 签发者 */
        private String issuer = "enterprise-hr-management";
    }

    @Data
    public static class Security {
        /** 连续登录失败达到该次数后锁定账号 */
        private int maxLoginFail = 5;
        /** 锁定时长（分钟） */
        private int lockMinutes = 30;
        /** 重置后的初始密码 */
        private String initPassword = "Init@2026";
        /** 免鉴权路径（Ant 风格） */
        private List<String> permitUrls = new ArrayList<>();
    }

    @Data
    public static class Business {
        /** 合同到期预警天数 */
        private int contractWarnDays = 60;
        /** 假期余额年度 */
        private int leaveBalanceYear = 2026;
        /** 上班时间，用于迟到判定 */
        private String workStartTime = "09:00:00";
        /** 下班时间，用于早退判定 */
        private String workEndTime = "18:00:00";
        /** 迟到宽限分钟数 */
        private int lateThresholdMinute = 5;
        /** 个税起征点 */
        private double taxThreshold = 5000D;
    }
}
