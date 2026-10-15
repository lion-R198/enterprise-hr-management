package com.enterprise.hr.salary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SalaryItem实体（对应表 {@code salary_item}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("salary_item")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 月度薪资记录ID */
    private Long salaryRecordId;

    /** 项目类型:1应发项 2扣款项 3企业承担项 */
    private Integer itemType;

    /** 项目编码,如 BASE/POST/OT/SOCIAL */
    private String itemCode;

    /** 项目名称,如 基本工资 */
    private String itemName;

    /** 项目金额 */
    private BigDecimal itemAmount;

    /** 展示排序,升序 */
    private Integer sortNo;

    /** 备注(计算口径说明) */
    private String remark;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
