package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 招聘需求审批入参（{@code PUT /recruit-requests/{id}/approve}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class RecruitApprovalReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批结果：2 通过 / 3 驳回 */
    @NotNull(message = "审批结果不能为空")
    private Integer result;
}
