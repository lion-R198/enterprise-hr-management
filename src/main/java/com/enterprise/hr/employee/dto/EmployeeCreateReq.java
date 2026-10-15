package com.enterprise.hr.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增员工（入职办理）入参。
 *
 * <p>校验注解放在这里而非 Entity 上：Entity 只是 ORM 映射，
 * 若把 {@code @NotBlank} 放到 Entity，PUT 局部更新会因未传的必填字段被拒。
 *
 * <p><b>工号由服务端生成（BR-02-01）且不可修改</b>：
 * {@code empNo} 不是必填项，服务端取"现有最大工号序号 + 1"自动分配（如 E0092），
 * 并依靠 {@code employee.emp_no} 唯一键 + 重试兜底并发撞号。
 * 仅当需要导入历史档案时允许显式传入（传了就必须全局唯一）。
 *
 * @author Enterprise HR Team
 */
@Data
public class EmployeeCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工号：<b>可选</b>，工号由服务端生成（BR-02-01）。
     * 不传时自动取号；传了则作为指定工号，仍需保证全局唯一。
     */
    @Size(max = 32, message = "工号长度不能超过 32")
    private String empNo;

    /** 姓名：必填 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过 32")
    private String name;

    /** 性别：M 男 / F 女 / U 未知 */
    private String gender;

    /** 年龄 */
    @Min(value = 0, message = "年龄不能为负")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;

    /** 身份证号（入库前由服务端脱敏展示，明文不出接口） */
    @Size(max = 32, message = "身份证号长度不能超过 32")
    private String idCard;

    /** 邮箱 */
    @Email(message = "邮箱格式错误")
    private String email;

    /** 手机号 */
    private String phone;

    /** 最高学历：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 其他 */
    private Integer education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 所学专业 */
    private String major;

    /** 所属部门 ID：必填 */
    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    /** 所属岗位 ID：必填 */
    @NotNull(message = "所属岗位不能为空")
    private Long positionId;

    /** 职位名称（兼容字段；不传时服务端按 positionId 取岗位名快照） */
    private String position;

    /** 职级，如 P5/M2 */
    private String jobLevel;

    /** 用工性质：1 正式 / 2 试用 / 3 实习 / 4 兼职 / 5 劳务，默认 1 */
    private Integer employeeType;

    /** 入职日期，不传取当天 */
    private LocalDate hireDate;

    /** 直属主管员工 ID */
    private Long managerId;

    /** 薪资 */
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
