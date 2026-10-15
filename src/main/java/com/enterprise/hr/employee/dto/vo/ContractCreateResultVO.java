package com.enterprise.hr.employee.dto.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 劳动合同签订结果响应。
 *
 * <p>契约：{@code { success, id, contractNo }}。合同编号由服务端按
 * {@code CT + 年份 + 5 位流水} 生成，前端需要立即回显，故
 * {@code contractNo} 用 {@link JsonProperty} 保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContractCreateResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 新合同主键 */
    private Long id;

    /** 合同编号，如 CT202600001 */
    @JsonProperty("contractNo")
    private String contractNo;

    public static ContractCreateResultVO ok(Long id, String contractNo) {
        return new ContractCreateResultVO(Boolean.TRUE, id, contractNo);
    }
}
