package com.enterprise.hr.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务单号生成工具。
 *
 * <p>单号规则：{@code 前缀 + 年份 + 5 位流水}，例如 {@code LV202600123}。
 *
 * <p>为什么不用 UUID 或时间戳：
 * <ul>
 *     <li>人事单据需要人工口头/书面流转，单号必须简短可读、可排序</li>
 *     <li>年度重置流水符合企业"按年归档"的票据管理习惯</li>
 * </ul>
 *
 * <p>流水号由调用方传入（通常取"当年同前缀已有单据数 + 1"），
 * 本工具只负责格式化，保证编号规则全局唯一实现，不散落在各 Service 里。
 * 并发下如发生编号撞车，由数据库唯一键 + 上层重试兜底。
 *
 * @author Enterprise HR Team
 */
public final class BizNoGenerator {

    /** 流水号位数 */
    private static final int SEQ_WIDTH = 5;

    private BizNoGenerator() {
    }

    /**
     * 生成单号。
     *
     * @param prefix 业务前缀，如 LV(请假) / OT(加班) / BT(出差) / RC(招聘需求) / CT(合同) / TP(培训) / PF(考核)
     * @param seq    当年流水序号，从 1 开始
     */
    public static String next(String prefix, long seq) {
        return next(prefix, LocalDate.now(), seq);
    }

    /** 指定年份生成（便于单测与数据修复） */
    public static String next(String prefix, LocalDate date, long seq) {
        String year = date.format(DateTimeFormatter.ofPattern("yyyy"));
        long safeSeq = seq < 1 ? 1 : seq;
        String body = String.valueOf(safeSeq);
        if (body.length() > SEQ_WIDTH) {
            // 流水溢出位数时不再截断（截断会导致重复），直接顺延位数
            return prefix + year + body;
        }
        return prefix + year + "0".repeat(SEQ_WIDTH - body.length()) + body;
    }

    /** 当年前缀匹配串，用于 {@code LIKE 'LV2026%'} 统计已有单量 */
    public static String yearPrefix(String prefix) {
        return prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
    }

    /**
     * 员工工号：{@code E + 4 位序号}，如 {@code E0001}。
     * 工号由系统生成且不可修改（BR-02-01）。
     */
    public static String empNo(long seq) {
        long safeSeq = seq < 1 ? 1 : seq;
        String body = String.valueOf(safeSeq);
        if (body.length() >= 4) {
            return "E" + body;
        }
        return "E" + "0".repeat(4 - body.length()) + body;
    }

    /**
     * 解析工号尾部序号，用于"取最大工号 +1"。
     *
     * @return 解析失败返回 0（调用方视为无效工号，跳过）
     */
    public static long parseEmpNoSeq(String empNo) {
        if (empNo == null) {
            return 0L;
        }
        String digits = empNo.trim().replaceAll("^[A-Za-z]+", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
