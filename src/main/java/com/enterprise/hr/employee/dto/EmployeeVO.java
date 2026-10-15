package com.enterprise.hr.employee.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工出参 VO。
 *
 * <p><b>字段命名</b>：蛇形（{@code emp_no} / {@code hire_date} / {@code dept_name}），
 * 与数据库列名一致，前端 {@code employee/index.vue} 的 {@code prop="emp_no"} 直接绑定。
 *
 * <p><b>刻意不包含</b> {@code deleted} / {@code created_at} / {@code updated_at}
 * 等运营字段 —— 对外接口只暴露业务语义字段，避免内部实现细节泄漏到前端。
 *
 * <p><b>敏感信息</b>：{@code id_card} / {@code bank_account} 一律由服务端
 * 经 {@code MaskUtil} 脱敏后再放入 VO（NF-08），前端拿不到明文。
 *
 * <p><b>关联字段</b>：{@code dept_name}（所属部门名）与 {@code manager_name}（直属主管姓名）
 * 由 Service 批量补齐，前端列表与详情抽屉都直接展示，不做二次请求。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 工号（系统生成，不可修改 BR-02-01） */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别：M / F / U */
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 身份证号（已脱敏，如 110219**********01） */
    private String idCard;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 现居住地址 */
    private String address;

    /** 最高学历：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 其他 */
    private Integer education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 所学专业 */
    private String major;

    /** 所属部门 ID */
    private Long deptId;

    /** 所属部门名（关联补齐） */
    private String deptName;

    /** 所属岗位 ID */
    private Long positionId;

    /** 职位名称（冗余快照） */
    private String position;

    /** 职级，如 P5/M2 */
    private String jobLevel;

    /** 用工性质：1 正式 / 2 试用 / 3 实习 / 4 兼职 / 5 劳务 */
    private Integer employeeType;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 转正日期 */
    private LocalDate regularDate;

    /** 离职日期 */
    private LocalDate leaveDate;

    /** 离职原因 */
    private String leaveReason;

    /** 在职状态：0 离职 / 1 在职 / 2 试用 / 3 待入职 */
    private Integer status;

    /** 直属主管员工 ID */
    private Long managerId;

    /** 直属主管姓名（关联补齐） */
    private String managerName;

    /** 薪资 */
    private BigDecimal salary;

    /** 开户银行 */
    private String bankName;

    /** 银行卡号（已脱敏，如 6222***********001） */
    private String bankAccount;

    /** 头像地址 */
    private String avatar;

    /** 备注 */
    private String remark;
}
