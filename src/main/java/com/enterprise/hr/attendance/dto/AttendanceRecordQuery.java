package com.enterprise.hr.attendance.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 打卡记录分页查询入参（F-04-01，{@code GET /attendance/records}）。
 *
 * <p>查询参数统一驼峰命名（前端 {@code ?current=1&size=14&startDate=...}），
 * 与 {@link PageQuery} 的 {@code current} / {@code size} 语义保持一致。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceRecordQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 员工ID（人事/主管按人筛选；普通员工忽略该参数，强制仅看本人） */
    private Long employeeId;

    /** 考勤日期起（含） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /** 考勤日期止（含） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** 考勤结果：1 正常 / 2 迟到 / 3 早退 / 4 缺卡 / 5 旷工 / 6 休假 / 7 出差 / 8 调休 */
    private Integer status;
}
