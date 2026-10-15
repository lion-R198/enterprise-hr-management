package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 招聘模块通用写操作结果（新增候选人 / 面试记录）。
 *
 * <p>契约：{@code { success: true, id: 123 }}。
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
