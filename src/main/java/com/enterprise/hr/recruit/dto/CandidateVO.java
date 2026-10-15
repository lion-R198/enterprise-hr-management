package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 候选人响应 VO。
 *
 * <p>{@code request_no} / {@code position_name} 取自关联招聘需求（快照），
 * 供前端「应聘岗位」列与需求下拉直接展示。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CandidateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 关联需求单号（联表展示字段） */
    private String requestNo;

    /** 应聘岗位名称（取自关联需求快照） */
    private String positionName;

    /** 候选人姓名 */
    private String name;

    /** 性别：M 男 / F 女 / U 未知 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 最高学历：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 其他 */
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

    /** 简历来源：1 官网 / 2 招聘网站 / 3 内部推荐 / 4 猎头 / 5 校园招聘 / 6 其他 */
    private Integer source;

    /** 内推人员工ID */
    private Long referrerId;

    /** 所处阶段：1 待筛选 / 2 待面试 / 3 面试中 / 4 待录用 / 5 已录用 / 6 已淘汰 */
    private Integer stage;

    /** 是否纳入人才库：1 是 / 0 否 */
    private Integer isTalent;

    /** 人才库标签，逗号分隔 */
    private String talentTags;

    /** 备注（淘汰原因等） */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
