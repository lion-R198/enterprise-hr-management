
package com.enterprise.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工实体（对应表 {@code employee}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工工号（唯一，不可修改），如 E0001 */
    @TableField("emp_no")
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别：M 男 / F 女 / U 未知 */
    private String gender;

    /** 年龄（冗余快照，可由 birth_date 推算） */
    private Integer age;

    /** 出生日期 */
    @TableField("birth_date")
    private LocalDate birthDate;

    /** 身份证号（加密存储） */
    @TableField("id_card")
    private String idCard;

    /** 民族，如 汉族 */
    private String nation;

    /** 籍贯 */
    @TableField("native_place")
    private String nativePlace;

    /** 婚姻状况：1 未婚 / 2 已婚 / 3 离异 / 4 丧偶 */
    @TableField("marital_status")
    private Integer maritalStatus;

    /** 政治面貌：1 群众 / 2 共青团员 / 3 中共党员 / 4 民主党派 */
    @TableField("political_status")
    private Integer politicalStatus;

    /** 手机号 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 紧急联系人姓名 */
    @TableField("emergency_contact")
    private String emergencyContact;

    /** 紧急联系人电话 */
    @TableField("emergency_phone")
    private String emergencyPhone;

    /** 现居住地址 */
    private String address;

    /** 最高学历：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 其他 */
    private Integer education;

    /** 毕业院校 */
    @TableField("graduate_school")
    private String graduateSchool;

    /** 所学专业 */
    private String major;

    /** 毕业日期 */
    @TableField("graduate_date")
    private LocalDate graduateDate;

    /** 所属部门ID */
    @TableField("dept_id")
    private Long deptId;

    /** 所属岗位ID */
    @TableField("position_id")
    private Long positionId;

    /** 职位名称（冗余快照，避免列表频繁联表） */
    private String position;

    /** 职级，如 P5/M2 */
    @TableField("job_level")
    private String jobLevel;

    /** 用工性质：1 正式 / 2 试用 / 3 实习 / 4 兼职 / 5 劳务 */
    @TableField("employee_type")
    private Integer employeeType;

    /** 入职日期 */
    @TableField("hire_date")
    private LocalDate hireDate;

    /** 转正日期 */
    @TableField("regular_date")
    private LocalDate regularDate;

    /** 参加工作时间 */
    @TableField("work_start_date")
    private LocalDate workStartDate;

    /** 离职日期 */
    @TableField("leave_date")
    private LocalDate leaveDate;

    /** 离职原因 */
    @TableField("leave_reason")
    private String leaveReason;

    /** 在职状态：0 离职 / 1 在职 / 2 试用 / 3 待入职 */
    private Integer status;

    /** 直属主管（员工ID） */
    @TableField("manager_id")
    private Long managerId;

    /** 当前基本薪资（冗余快照，明细见 employee_salary） */
    private BigDecimal salary;

    /** 开户银行 */
    @TableField("bank_name")
    private String bankName;

    /** 银行卡号（加密存储） */
    @TableField("bank_account")
    private String bankAccount;

    /** 头像地址 */
    private String avatar;

    /** 备注 */
    private String remark;

    /** 创建人用户ID */
    @TableField("created_by")
    private Long createdBy;

    /** 最后修改人用户ID */
    @TableField("updated_by")
    private Long updatedBy;

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