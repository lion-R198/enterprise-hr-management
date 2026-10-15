package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 薪资明细项出参 VO（salary_item）。
 *
 * <p>前端薪资条弹窗按 {@code item_type} 分组渲染（1 应发项 / 2 扣款项 / 3 企业承担项），
 * 组内按 {@code sort_no} 升序展示，故返回顺序即展示顺序。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 月度薪资记录 ID */
    private Long salaryRecordId;

    /** 项目类型：1 应发项 / 2 扣款项 / 3 企业承担项 */
    private Integer itemType;

    /** 项目编码，如 BASE/POST/OT/SOCIAL */
    private String itemCode;

    /** 项目名称，如 基本工资 */
    private String itemName;

    /** 项目金额 */
    private BigDecimal itemAmount;

    /** 展示排序，升序 */
    private Integer sortNo;

    /** 备注（计算口径说明） */
    private String remark;
}
