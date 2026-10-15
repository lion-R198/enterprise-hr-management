package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 推进候选人阶段入参（{@code PUT /candidates/{id}/stage}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class CandidateStageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标阶段：1 待筛选 / 2 待面试 / 3 面试中 / 4 待录用 / 5 已录用 / 6 已淘汰 */
    @NotNull(message = "目标阶段不能为空")
    private Integer stage;

    /** 备注（淘汰时记录淘汰原因） */
    private String remark;
}
