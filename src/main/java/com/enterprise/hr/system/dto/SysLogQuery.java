package com.enterprise.hr.system.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 日志分页查询条件（登录日志与操作日志共用）。
 *
 * <p>{@code status} 的语义随日志类型变化：登录日志里是 {@code login_status}（1 成功 0 失败），
 * 操作日志里是 {@code oper_status}（1 成功 0 失败）——两者取值语义一致，
 * 因此共用一个字段名，前端切换 Tab 时无需改参数名。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 操作人/登录账号，模糊匹配 */
    private String username;

    /** 结果状态：1 成功 / 0 失败 */
    private Integer status;

    /** 所属模块（仅操作日志有效），如「员工信息」 */
    private String module;

    /** 起始日期（含当天 00:00:00） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /** 结束日期（含当天 23:59:59） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
