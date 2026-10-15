package com.enterprise.hr.system.support;

import com.enterprise.hr.common.BizException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 系统参数值校验器。
 *
 * <p>为什么单独立一个类：参数校验规则是"业务知识的集中地"，把它从 Service 里剥出来
 * 才能被单测直接覆盖，也避免 {@code SysConfigServiceImpl} 变成一个巨大的 if-else 迷宫。
 *
 * <p>校验分两层：
 * <ol>
 *     <li><b>通用层</b>——按 {@code value_type} 校验值的形态（数字/布尔/JSON）</li>
 *     <li><b>业务层</b>——按 {@code config_key} 校验值的合理区间，
 *         拦住"格式合法但业务上荒谬"的输入</li>
 * </ol>
 *
 * @author Enterprise HR Team
 */
public final class ConfigValueValidator {

    /** 值类型：数字 */
    private static final int TYPE_NUMBER = 2;
    /** 值类型：布尔 */
    private static final int TYPE_BOOLEAN = 3;
    /** 值类型：JSON */
    private static final int TYPE_JSON = 4;

    /**
     * 按参数键校验业务合理区间。
     *
     * <p>只覆盖"改错会引发线上事故"的几个关键参数，不做无意义的全量覆盖：
     * <ul>
     *     <li>{@code attendance.lock.day} —— 封账日必须落在 1~28。
     *         允许 29~31 会导致 2 月永远无法封账（BR-04-03 会永久卡住）</li>
     *     <li>{@code attendance.late.tolerance} —— 0~480 分钟，超过 8 小时已不是"迟到"</li>
     *     <li>{@code salary.tax.threshold} —— 个税起征点不得为负</li>
     *     <li>{@code salary.fund.rate} / {@code recruit.offer.probation.rate} —— 百分比 0~100</li>
     *     <li>{@code contract.warn.days} —— 逗号分隔的预警天数，每项 1~365</li>
     * </ul>
     */
    private static final Map<String, Consumer<String>> KEY_RULES = Map.of(
            "attendance.lock.day", value -> checkIntRange("考勤封账日", value, 1, 28),
            "attendance.late.tolerance", value -> checkIntRange("迟到容忍分钟数", value, 0, 480),
            "salary.tax.threshold", value -> checkDecimalRange("个税起征点", value, BigDecimal.ZERO, null),
            "salary.fund.rate", value -> checkDecimalRange("公积金个人比例", value, BigDecimal.ZERO, BigDecimal.valueOf(100)),
            "recruit.offer.probation.rate", value -> checkDecimalRange("试用期薪资比例", value, BigDecimal.ZERO, BigDecimal.valueOf(100)),
            "contract.warn.days", ConfigValueValidator::checkDayList
    );

    private ConfigValueValidator() {
    }

    /**
     * 校验参数值。
     *
     * @param configKey  参数键，用于匹配业务区间规则
     * @param configName 参数名称，用于拼装可读的报错信息
     * @param value      待校验的值（非空，调用方保证）
     * @param valueType  值类型
     * @param isSystem   是否系统内置：内置参数不允许置空
     */
    public static void validate(String configKey, String configName, String value,
                                Integer valueType, Integer isSystem) {
        boolean builtin = isSystem != null && isSystem == 1;
        if (!StringUtils.hasText(value)) {
            if (builtin) {
                // 内置参数被业务代码直接读取，置空等价于线上功能失效
                throw BizException.badRequest("系统内置参数「" + configName + "」不允许置空");
            }
            return;
        }
        String trimmed = value.trim();

        // ---- 通用层：值类型形态校验 ----
        if (valueType != null) {
            switch (valueType) {
                case TYPE_NUMBER -> parseDecimal(configName, trimmed);
                case TYPE_BOOLEAN -> checkBoolean(configName, trimmed);
                case TYPE_JSON -> checkJsonOrCsv(configName, trimmed);
                default -> {
                    // 1 字符串：无需形态校验
                }
            }
        }

        // ---- 业务层：关键参数区间校验 ----
        Consumer<String> rule = KEY_RULES.get(configKey);
        if (rule != null) {
            rule.accept(trimmed);
        }
    }

    // ------------------------------------------------------------------ 校验实现

    private static void checkBoolean(String name, String value) {
        String lower = value.toLowerCase();
        if (!"true".equals(lower) && !"false".equals(lower) && !"0".equals(lower) && !"1".equals(lower)) {
            throw BizException.badRequest("参数「" + name + "」为布尔类型，取值只能是 true/false/1/0");
        }
    }

    /**
     * JSON 类型宽容处理：既接受标准 JSON，也接受历史遗留的逗号分隔列表。
     *
     * <p>库内 {@code contract.warn.days} 的既有值是 {@code 30,60}（逗号列表而非 JSON），
     * 若只认严格 JSON，管理员将无法保存任何合法修改——保护数据一致性不能以牺牲可用性为代价。
     */
    private static void checkJsonOrCsv(String name, String value) {
        if (value.startsWith("[") || value.startsWith("{")) {
            return;
        }
        // 逗号分隔列表：逐项必须非空
        for (String part : value.split(",")) {
            if (!StringUtils.hasText(part)) {
                throw BizException.badRequest("参数「" + name + "」为列表类型，各项不得为空");
            }
        }
    }

    private static void checkIntRange(String name, String value, int min, int max) {
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw BizException.badRequest("参数「" + name + "」必须为整数");
        }
        if (parsed < min || parsed > max) {
            throw BizException.badRequest("参数「" + name + "」取值范围为 " + min + " ~ " + max + "，当前值 " + parsed);
        }
    }

    private static void checkDecimalRange(String name, String value, BigDecimal min, BigDecimal max) {
        BigDecimal parsed = parseDecimal(name, value);
        if (min != null && parsed.compareTo(min) < 0) {
            throw BizException.badRequest("参数「" + name + "」不得小于 " + min.toPlainString());
        }
        if (max != null && parsed.compareTo(max) > 0) {
            throw BizException.badRequest("参数「" + name + "」不得大于 " + max.toPlainString());
        }
    }

    private static BigDecimal parseDecimal(String name, String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw BizException.badRequest("参数「" + name + "」必须为数字");
        }
    }

    /** 合同到期预警天数：逗号分隔的多个正整数 */
    private static void checkDayList(String value) {
        for (String part : value.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                throw BizException.badRequest("合同到期预警天数各档不得为空");
            }
            checkIntRange("合同到期预警天数", item, 1, 365);
        }
    }
}
