package com.enterprise.hr.employee.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用「写操作成功」响应（员工修改 / 删除 / 状态变更 / 部门删除等）。
 *
 * <p>契约：{@code { success: true }}，与前端 mock 及考勤、招聘等模块保持一致，
 * 前端只判断 HTTP 200 与 {@code code === 200}，不解析该结构。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SimpleResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 成功结果常量 */
    public static SimpleResultVO ok() {
        return new SimpleResultVO(Boolean.TRUE);
    }
}
