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
 * InsuranceRecord实体（对应表 {@code insurance_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InsuranceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 关联社保配置ID */
    private Long configId;

    /** 缴纳年份 */
    private Integer recordYear;

    /** 缴纳月份:1-12 */
    private Integer recordMonth;

    /** 社保缴费基数 */
    private BigDecimal socialBase;

    /** 公积金缴费基数 */
    private BigDecimal fundBase;

    /** 养老保险-个人 */
    private BigDecimal pensionPersonal;

    /** 养老保险-企业 */
    private BigDecimal pensionCompany;

    /** 医疗保险-个人 */
    private BigDecimal medicalPersonal;

    /** 医疗保险-企业 */
    private BigDecimal medicalCompany;

    /** 失业保险-个人 */
    private BigDecimal unemployPersonal;

    /** 失业保险-企业 */
    private BigDecimal unemployCompany;

    /** 工伤保险-企业(个人不缴) */
    private BigDecimal injuryCompany;

    /** 生育保险-企业(个人不缴) */
    private BigDecimal maternityCompany;

    /** 住房公积金-个人 */
    private BigDecimal fundPersonal;

    /** 住房公积金-企业 */
    private BigDecimal fundCompany;

    /** 个人缴纳合计(含公积金) */
    private BigDecimal totalPersonal;

    /** 企业缴纳合计(含公积金) */
    private BigDecimal totalCompany;

    /** 缴纳状态:0未缴 1已缴 2已减员 */
    private Integer payStatus;

    /** 备注 */
    private String remark;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
