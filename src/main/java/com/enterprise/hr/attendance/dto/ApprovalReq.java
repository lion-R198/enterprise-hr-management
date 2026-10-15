package com.enterprise.hr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用审批入参（请假 / 加班 / 出差审批共用）。
 *
 * <p>请求体使用驼峰命名（不加 {@code @JsonNaming}），前端发送 {@code { result: 2, opinion: '同意' }}。
 *
 * @author Enterprise HR Team
 */
@Data
public class ApprovalReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批结果：2 通过 / 3 驳回（其它值由 Service 拒绝） */
    @NotNull(message = "审批结果不能为空")
    private Integer result;

    /** 审批意见，选填 */
    private String opinion;
}
