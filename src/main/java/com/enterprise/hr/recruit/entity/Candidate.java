package com.enterprise.hr.recruit.entity;

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
 * Candidate实体（对应表 {@code candidate}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("candidate")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Candidate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 候选人姓名 */
    private String name;

    /** 性别:M男 F女 U未知 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 最高学历:1博士 2硕士 3本科 4大专 5中专 6高中 7其他 */
    private Integer education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 所学专业 */
    private String major;

    /** 工作年限 */
    private Integer workYears;

    /** 当前就职公司 */
    private String currentCompany;

    /** 当前职位 */
    private String currentPosition;

    /** 期望薪资 */
    private BigDecimal expectSalary;

    /** 简历文件地址 */
    private String resumeUrl;

    /** 简历来源:1官网 2招聘网站 3内部推荐 4猎头 5校园招聘 6其他 */
    private Integer source;

    /** 内推人员工ID */
    private Long referrerId;

    /** 所处阶段:1待筛选 2待面试 3面试中 4待录用 5已录用 6已淘汰 */
    private Integer stage;

    /** 是否纳入人才库:1是 0否 */
    private Integer isTalent;

    /** 人才库标签,逗号分隔,如 Java,后端,3年经验 */
    private String talentTags;

    /** 备注(淘汰原因等) */
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
