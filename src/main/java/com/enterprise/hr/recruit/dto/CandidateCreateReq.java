package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 录入候选人入参（{@code POST /candidates}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class CandidateCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联招聘需求ID */
    @NotNull(message = "关联招聘需求不能为空")
    private Long recruitRequestId;

    /** 候选人姓名 */
    @NotBlank(message = "候选人姓名不能为空")
    private String name;

    /** 性别：M 男 / F 女 / U 未知；空则按 U 处理 */
    private String gender;

    /** 最高学历：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 其他 */
    private Integer education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 工作年限 */
    private Integer workYears;

    /** 当前就职公司 */
    private String currentCompany;

    /** 当前职位 */
    private String currentPosition;

    /** 期望薪资 */
    private BigDecimal expectSalary;

    /** 简历来源：1 官网 / 2 招聘网站 / 3 内部推荐 / 4 猎头 / 5 校园招聘 / 6 其他；空则按 2 处理 */
    private Integer source;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 备注 */
    private String remark;
}
