package com.enterprise.hr.employee.entity;

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
 * 假别实体（对应表 {@code leave_type}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("leave_type")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LeaveType implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 假别编码（唯一），如 ANNUAL/SICK */
    @TableField("type_code")
    private String typeCode;

    /** 假别名称，如 年假 */
    @TableField("type_name")
    private String typeName;

    /** 是否带薪：1 带薪 / 0 不带薪 */
    @TableField("is_paid")
    private Integer isPaid;

    /** 计量单位：1 天 / 2 小时 */
    private Integer unit;

    /** 最小请假单位（天），如 0.5 表示半天 */
    @TableField("min_unit")
    private BigDecimal minUnit;

    /** 每年额度上限（0 = 不限） */
    @TableField("max_days_per_year")
    private BigDecimal maxDaysPerYear;

    /** 额度有效月数 */
    @TableField("valid_months")
    private Integer validMonths;

    /** 是否可结转下年：1 是 / 0 否 */
    @TableField("can_carry_over")
    private Integer canCarryOver;

    /** 是否需上传证明：1 是 / 0 否 */
    @TableField("need_proof")
    private Integer needProof;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
