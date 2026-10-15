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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社保公积金配置实体（对应表 {@code insurance_config}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_config")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InsuranceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 配置名称，如 2026年北京社保-养老 */
    @TableField("config_name")
    private String configName;

    /** 参保城市 */
    private String city;

    /** 险种类型：1 养老 / 2 医疗 / 3 失业 / 4 工伤 / 5 生育 / 6 住房公积金 */
    @TableField("social_type")
    private Integer socialType;

    /** 企业缴纳比例（如 0.1600 = 16%） */
    @TableField("company_rate")
    private BigDecimal companyRate;

    /** 个人缴纳比例（如 0.0800 = 8%） */
    @TableField("personal_rate")
    private BigDecimal personalRate;

    /** 缴费基数下限 */
    @TableField("base_min")
    private BigDecimal baseMin;

    /** 缴费基数上限 */
    @TableField("base_max")
    private BigDecimal baseMax;

    /** 生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

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
