package com.enterprise.hr.salary.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 薪资发放请求体：{@code {batchNo}}（可选）。
 *
 * <p>前端 {@code paySalaryRecord(id)} 不传 body，因此 Controller 用
 * {@code @RequestBody(required = false)} 接收；未指定批次号时由 Service 按
 * {@code PAY + yyyyMMdd + 001} 生成。
 *
 * @author Enterprise HR Team
 */
@Data
public class SalaryPayReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发放批次号（可选，缺省自动生成） */
    @Size(max = 64, message = "发放批次号长度不得超过 64")
    private String batchNo;
}
