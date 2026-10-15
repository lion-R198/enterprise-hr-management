package com.enterprise.hr.recruit.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 面试记录查询入参（F-06-04，{@code GET /interview-records}）。
 *
 * <p>该接口<b>非分页</b>：前端在候选人弹窗中一次展示某人的全部面试轮次，响应走
 * {@code PageResult.list(...)}，前端仍按 {@code records} / {@code total} 读取。
 *
 * @author Enterprise HR Team
 */
@Data
public class InterviewRecordQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 候选人ID；空则返回全部（受权限约束的调用方应始终传入） */
    private Long candidateId;
}
