package com.enterprise.hr.performance.service.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 绩效等级规则解析器（BR-08-03 等级自动判定）。
 *
 * <p>方案表 {@code performance_plan.grade_rule} 存的是人可读的区间串，典型形如：
 * {@code S:90+,A:80-89,B:70-79,C:60-69,D:<60}。本类把它解析成可判定的区间集合，
 * <b>阈值完全来自规则串本身，代码不写死任何分数边界</b>——HR 在页面上改规则即改口径。
 *
 * <p>支持的区间写法（大小写不敏感，允许空格与全角冒号）：
 * <table border="1">
 *     <caption>区间表达式</caption>
 *     <tr><th>写法</th><th>语义</th><th>示例</th></tr>
 *     <tr><td>{@code 90+}</td><td>大于等于 90</td><td>{@code S:90+}</td></tr>
 *     <tr><td>{@code >=90}</td><td>大于等于 90</td><td>{@code S:>=90}</td></tr>
 *     <tr><td>{@code >90}</td><td>大于 90（不含）</td><td>{@code S:>90}</td></tr>
 *     <tr><td>{@code 80-89}</td><td>80 ≤ 分 ≤ 89（闭区间）</td><td>{@code A:80-89}</td></tr>
 *     <tr><td>{@code <60}</td><td>小于 60</td><td>{@code D:&lt;60}</td></tr>
 *     <tr><td>{@code <=60}</td><td>小于等于 60</td><td>{@code D:&lt;=60}</td></tr>
 *     <tr><td>{@code 60}</td><td>单值按"下限"解释，等价于 {@code 60+}</td><td>{@code C:60}</td></tr>
 * </table>
 *
 * <p>判定顺序：<b>按规则串中出现的先后顺序取第一个命中的等级</b>，
 * 因此规则串应写成由高到低（{@code S → D}）；命中不到时按最接近的边界兜底，
 * 仍无法判定则落到规则串的最后一个等级。
 *
 * @author Enterprise HR Team
 */
public final class GradeRuleResolver {

    /** 未配置等级规则时的兜底规则（与前端 mock 的默认值一致） */
    public static final String DEFAULT_RULE = "S:90+,A:80-89,B:70-79,C:60-69,D:<60";

    private GradeRuleResolver() {
    }

    /**
     * 按规则串定档。
     *
     * @param gradeRule 规则串，为空/解析不出任何区间时回退到 {@link #DEFAULT_RULE}
     * @param score     最终得分，null 视为 0
     * @return 等级编码（如 {@code S} / {@code A} …）；规则串完全不可用时返回 {@code null}
     */
    public static String resolve(String gradeRule, BigDecimal score) {
        BigDecimal value = score == null ? BigDecimal.ZERO : score;
        List<Band> bands = parse(gradeRule);
        if (bands.isEmpty()) {
            // 规则串为空或格式完全无法识别：回退默认规则（与 mock 的硬编码阈值一致）
            bands = parse(DEFAULT_RULE);
        }
        if (bands.isEmpty()) {
            return null;
        }

        // 1) 按声明顺序取第一个命中区间：高等级在前，先命中先返回
        for (Band band : bands) {
            if (band.matches(value)) {
                return band.grade;
            }
        }

        // 2) 落在所有区间之外：低于最小下界 → 最低档；高于最大上界 → 最高档
        Band lowest = null;
        Band highest = null;
        for (Band band : bands) {
            if (band.min == null) {
                continue;
            }
            if (lowest == null || band.min.compareTo(lowest.min) < 0) {
                lowest = band;
            }
            if (highest == null || band.min.compareTo(highest.min) > 0) {
                highest = band;
            }
        }
        if (highest != null && value.compareTo(highest.min) > 0) {
            return highest.grade;
        }
        if (lowest != null) {
            return lowest.grade;
        }
        // 3) 兜底：最后一个等级（规则串通常由高到低书写）
        return bands.get(bands.size() - 1).grade;
    }

    /**
     * 解析规则串为区间列表。
     *
     * @return 解析成功的区间（保持书写顺序）；无法解析的片段被忽略
     */
    public static List<Band> parse(String gradeRule) {
        List<Band> bands = new ArrayList<>();
        if (gradeRule == null || gradeRule.isBlank()) {
            return bands;
        }
        // 兼容中文逗号 / 分号 / 全角冒号，避免 HR 手输规则时因标点导致整串失效
        String normalized = gradeRule.replace('，', ',')
                .replace('；', ',')
                .replace(';', ',')
                .replace('：', ':');
        for (String segment : normalized.split(",")) {
            String item = segment.trim();
            if (item.isEmpty()) {
                continue;
            }
            int colon = item.indexOf(':');
            if (colon <= 0 || colon == item.length() - 1) {
                continue;
            }
            String grade = item.substring(0, colon).trim();
            Band band = parseExpression(item.substring(colon + 1).trim());
            if (grade.isEmpty() || band == null) {
                continue;
            }
            band.grade = grade;
            bands.add(band);
        }
        return bands;
    }

    /** 解析单个区间表达式，无法识别返回 null */
    private static Band parseExpression(String expr) {
        if (expr.isEmpty()) {
            return null;
        }
        Band band = new Band();
        if (expr.startsWith("<=")) {
            BigDecimal max = number(expr.substring(2));
            if (max == null) {
                return null;
            }
            band.max = max;
            band.maxInclusive = true;
            return band;
        }
        if (expr.startsWith("<")) {
            BigDecimal max = number(expr.substring(1));
            if (max == null) {
                return null;
            }
            band.max = max;
            band.maxInclusive = false;
            return band;
        }
        if (expr.startsWith(">=")) {
            BigDecimal min = number(expr.substring(2));
            if (min == null) {
                return null;
            }
            band.min = min;
            band.minInclusive = true;
            return band;
        }
        if (expr.startsWith(">")) {
            BigDecimal min = number(expr.substring(1));
            if (min == null) {
                return null;
            }
            band.min = min;
            band.minInclusive = false;
            return band;
        }
        if (expr.endsWith("+")) {
            BigDecimal min = number(expr.substring(0, expr.length() - 1));
            if (min == null) {
                return null;
            }
            band.min = min;
            band.minInclusive = true;
            return band;
        }
        // 形如 80-89 的闭区间；分隔符必须出现在首位之后，避免把负号误判为分隔符
        int dash = expr.indexOf('-', 1);
        if (dash > 0) {
            BigDecimal min = number(expr.substring(0, dash));
            BigDecimal max = number(expr.substring(dash + 1));
            if (min == null || max == null) {
                return null;
            }
            band.min = min;
            band.minInclusive = true;
            band.max = max;
            band.maxInclusive = true;
            return band;
        }
        // 单值：按"下限"解释（60 等价于 60+）
        BigDecimal single = number(expr);
        if (single == null) {
            return null;
        }
        band.min = single;
        band.minInclusive = true;
        return band;
    }

    /** 去除百分号等装饰字符后转数值，失败返回 null */
    private static BigDecimal number(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replace("%", "").replace("分", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 单条等级区间。
     *
     * <p>{@code min} / {@code max} 为空表示该侧无界（如 {@code 90+} 只有下界、{@code <60} 只有上界）。
     */
    public static class Band {

        /** 等级编码 */
        private String grade;
        /** 下界，null = 无下界 */
        private BigDecimal min;
        /** 下界是否可取等 */
        private boolean minInclusive = true;
        /** 上界，null = 无上界 */
        private BigDecimal max;
        /** 上界是否可取等 */
        private boolean maxInclusive = true;

        /** 分数是否落在本区间 */
        public boolean matches(BigDecimal score) {
            if (min != null) {
                int cmp = score.compareTo(min);
                if (minInclusive ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (max != null) {
                int cmp = score.compareTo(max);
                if (maxInclusive ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            return true;
        }

        public String getGrade() {
            return grade;
        }
    }
}
