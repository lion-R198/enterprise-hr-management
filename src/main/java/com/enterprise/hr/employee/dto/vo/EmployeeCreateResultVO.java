package com.enterprise.hr.employee.dto.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 员工新增（入职办理）结果响应。
 *
 * <p>契约：{@code { success, id, empNo }}。工号由服务端生成（BR-02-01），
 * 前端新增后需要立即回显（{@code employee/index.vue} 提示"工号 xxx"），
 * 故该字段用 {@link JsonProperty} 显式覆盖蛇形策略保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeCreateResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新员工主键 */
    private Long id;

    /** 系统生成的工号，如 E0092（BR-02-01 不可修改） */
    @JsonProperty("empNo")
    private String empNo;

    public static EmployeeCreateResultVO ok(Long id, String empNo) {
        return new EmployeeCreateResultVO(Boolean.TRUE, id, empNo);
    }
}
