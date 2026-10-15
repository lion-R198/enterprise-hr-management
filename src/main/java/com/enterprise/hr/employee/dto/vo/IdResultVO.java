package com.enterprise.hr.employee.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用「新增成功」响应（部门新增 / 岗位新增 / 异动办理）。
 *
 * <p>契约：{@code { success: true, id: 123 }}，与前端 mock 的
 * {@code return { success: true, id }} 一致。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IdResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新记录主键 */
    private Long id;

    public static IdResultVO ok(Long id) {
        return new IdResultVO(Boolean.TRUE, id);
    }
}
