package com.enterprise.hr.performance.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 考核结果确认 / 申诉入参（BR-08-01，{@code PUT /performance-records/{id}/confirm}）。
 *
 * <p>请求体驼峰命名，字段名与前端 {@code api/performance.js#confirmPerfRecord} 一致。
 * {@code appeal = true} 时进入申诉分支，{@code reason} 落 {@code appeal_reason}。
 *
 * @author Enterprise HR Team
 */
@Data
public class ConfirmReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否申诉：true 申诉（等待复核），false / null 确认结果（流程完成） */
    private Boolean appeal;

    /** 申诉理由（仅申诉时有意义） */
    private String reason;
}
