package com.enterprise.hr.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 修改员工入参（局部更新语义）。
 *
 * <p>除"传了就要合法"的格式类校验（邮箱、年龄区间、薪资非负）外，
 * <b>不设 {@code @NotBlank}</b> —— 局部更新允许只传变更字段，
 * 未传字段在 Service 层被保护、不会被清空。
 *
 * <p>两点特殊约束：
 * <ul>
 *     <li>{@code empNo} 传入且与原值不一致时由 Service 拒绝（BR-02-01 工号不可修改）</li>
 *     <li>{@code status} 不在此接口生效：状态变更必须走 {@code PUT /employees/{id}/status}
 *         以便写入异动留痕（BR-02-04）</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Data
public class EmployeeUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工号：传入且与原值不同时由 Service 层拒绝（BR-02-01 工号不可修改） */
    @Size(max = 32, message = "工号长度不能超过 32")
    private String empNo;

    /** 姓名 */
    @Size(max = 32, message = "姓名长度不能超过 32")
    private String name;

    /** 性别 */
    private String gender;

    /** 年龄（传了就必须在合法区间） */
    @Min(value = 0, message = "年龄不能为负")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;

    /** 身份证号 */
    @Size(max = 32, message = "身份证号长度不能超过 32")
    private String idCard;

    /** 邮箱（传了就必须格式正确） */
    @Email(message = "邮箱格式错误")
    private String email;

    /** 手机号 */
    private String phone;

    /** 现居住地址 */
    private String address;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系人电话 */
    private String emergencyPhone;

    /** 婚姻状况：1 未婚 / 2 已婚 / 3 离异 / 4 丧偶 */
    private Integer maritalStatus;

    /** 政治面貌：1 群众 / 2 共青团员 / 3 中共党员 / 4 民主党派 */
    private Integer politicalStatus;

    /** 最高学历 */
    private Integer education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 所学专业 */
    private String major;

    /** 所属部门 ID */
    private Long deptId;

    /** 所属岗位 ID（变更时同步刷新 position 冗余快照） */
    private Long positionId;

    /** 职位名称（兼容字段，通常由 positionId 派生） */
    private String position;

    /** 职级 */
    private String jobLevel;

    /** 用工性质 */
    private Integer employeeType;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 直属主管员工 ID；传 0 表示清空主管（null = 本次不修改，局部更新语义） */
    private Long managerId;

    /** 薪资（传了就不能为负，BR-02-02） */
    @DecimalMin(value = "0.00", message = "薪资不能为负")
    private BigDecimal salary;

    /** 开户银行 */
    private String bankName;

    /** 银行卡号 */
    @Size(max = 32, message = "银行卡号长度不能超过 32")
    private String bankAccount;

    /** 备注 */
    private String remark;
}
