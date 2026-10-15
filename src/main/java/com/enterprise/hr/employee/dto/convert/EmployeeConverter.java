package com.enterprise.hr.employee.dto.convert;

import com.enterprise.hr.common.util.MaskUtil;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.entity.Employee;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工 DTO / Entity / VO 转换工具。
 *
 * <p>字段数量不多时手写转换的可读性与可控性最好（明确知道每个字段怎么走），
 * 字段规模膨胀到几十个再考虑引入 MapStruct。
 *
 * <p>转换器只做"字段搬运"，不查库、不判业务规则：
 * {@code dept_name} / {@code manager_name} 等关联字段由 Service 批量补齐，
 * 业务校验（工号不可改、薪资非负等）留在 Service。
 *
 * @author Enterprise HR Team
 */
public final class EmployeeConverter {

    private EmployeeConverter() {
        // 工具类禁止实例化
    }

    /**
     * 新增入参 → 实体。
     *
     * <p>{@code status}（初始待入职）与 {@code empNo}（BR-02-01 服务端生成）
     * 由 Service 决定，这里不赋值。
     */
    public static Employee toEntity(EmployeeCreateReq req) {
        if (req == null) {
            return null;
        }
        Employee e = new Employee();
        e.setEmpNo(trim(req.getEmpNo()));
        e.setName(trim(req.getName()));
        e.setGender(req.getGender());
        e.setAge(req.getAge());
        e.setIdCard(trim(req.getIdCard()));
        e.setEmail(trim(req.getEmail()));
        e.setPhone(trim(req.getPhone()));
        e.setEducation(req.getEducation());
        e.setGraduateSchool(trim(req.getGraduateSchool()));
        e.setMajor(trim(req.getMajor()));
        e.setDeptId(req.getDeptId());
        e.setPositionId(req.getPositionId());
        e.setPosition(trim(req.getPosition()));
        e.setJobLevel(trim(req.getJobLevel()));
        e.setEmployeeType(req.getEmployeeType());
        e.setHireDate(req.getHireDate());
        e.setManagerId(normalizeManagerId(req.getManagerId()));
        e.setSalary(req.getSalary());
        e.setBankName(trim(req.getBankName()));
        e.setBankAccount(trim(req.getBankAccount()));
        e.setRemark(trim(req.getRemark()));
        return e;
    }

    /**
     * 局部更新：只覆盖请求中<b>非 null</b> 的字段。
     * null 语义为"本次不修改"，因此不会把数据库原有值清空。
     *
     * <p>注意两点：
     * <ul>
     *     <li>{@code empNo} 不在此处覆盖 —— 工号不可修改（BR-02-01），由 Service 显式拒绝</li>
     *     <li>{@code managerId} 传 0 表示"清空直属主管"（null 无法与"未传"区分）</li>
     * </ul>
     */
    public static void applyUpdate(EmployeeUpdateReq req, Employee target) {
        if (req == null || target == null) {
            return;
        }
        if (req.getName() != null) {
            target.setName(trim(req.getName()));
        }
        if (req.getGender() != null) {
            target.setGender(req.getGender());
        }
        if (req.getAge() != null) {
            target.setAge(req.getAge());
        }
        if (req.getIdCard() != null) {
            target.setIdCard(trim(req.getIdCard()));
        }
        if (req.getEmail() != null) {
            target.setEmail(trim(req.getEmail()));
        }
        if (req.getPhone() != null) {
            target.setPhone(trim(req.getPhone()));
        }
        if (req.getAddress() != null) {
            target.setAddress(trim(req.getAddress()));
        }
        if (req.getEmergencyContact() != null) {
            target.setEmergencyContact(trim(req.getEmergencyContact()));
        }
        if (req.getEmergencyPhone() != null) {
            target.setEmergencyPhone(trim(req.getEmergencyPhone()));
        }
        if (req.getMaritalStatus() != null) {
            target.setMaritalStatus(req.getMaritalStatus());
        }
        if (req.getPoliticalStatus() != null) {
            target.setPoliticalStatus(req.getPoliticalStatus());
        }
        if (req.getEducation() != null) {
            target.setEducation(req.getEducation());
        }
        if (req.getGraduateSchool() != null) {
            target.setGraduateSchool(trim(req.getGraduateSchool()));
        }
        if (req.getMajor() != null) {
            target.setMajor(trim(req.getMajor()));
        }
        if (req.getDeptId() != null) {
            target.setDeptId(req.getDeptId());
        }
        if (req.getPositionId() != null) {
            target.setPositionId(req.getPositionId());
        }
        if (req.getPosition() != null) {
            target.setPosition(trim(req.getPosition()));
        }
        if (req.getJobLevel() != null) {
            target.setJobLevel(trim(req.getJobLevel()));
        }
        if (req.getEmployeeType() != null) {
            target.setEmployeeType(req.getEmployeeType());
        }
        if (req.getHireDate() != null) {
            target.setHireDate(req.getHireDate());
        }
        if (req.getManagerId() != null) {
            target.setManagerId(normalizeManagerId(req.getManagerId()));
        }
        if (req.getSalary() != null) {
            target.setSalary(req.getSalary());
        }
        if (req.getBankName() != null) {
            target.setBankName(trim(req.getBankName()));
        }
        if (req.getBankAccount() != null) {
            target.setBankAccount(trim(req.getBankAccount()));
        }
        if (req.getRemark() != null) {
            target.setRemark(trim(req.getRemark()));
        }
    }

    /**
     * 实体 → 出参 VO。
     *
     * <p>身份证与银行卡在这里脱敏（NF-08）：只有"查询结果里本来就没有明文"，
     * 才能真正避免通过浏览器调试工具、接口重放拿到敏感数据。
     */
    public static EmployeeVO toVO(Employee e) {
        if (e == null) {
            return null;
        }
        return EmployeeVO.builder()
                .id(e.getId())
                .empNo(e.getEmpNo())
                .name(e.getName())
                .gender(e.getGender())
                .age(e.getAge())
                .idCard(MaskUtil.maskIdCard(e.getIdCard()))
                .email(e.getEmail())
                .phone(e.getPhone())
                .address(e.getAddress())
                .education(e.getEducation())
                .graduateSchool(e.getGraduateSchool())
                .major(e.getMajor())
                .deptId(e.getDeptId())
                .positionId(e.getPositionId())
                .position(e.getPosition())
                .jobLevel(e.getJobLevel())
                .employeeType(e.getEmployeeType())
                .hireDate(e.getHireDate())
                .regularDate(e.getRegularDate())
                .leaveDate(e.getLeaveDate())
                .leaveReason(e.getLeaveReason())
                .status(e.getStatus())
                .managerId(e.getManagerId())
                .salary(e.getSalary())
                .bankName(e.getBankName())
                .bankAccount(MaskUtil.maskBankAccount(e.getBankAccount()))
                .avatar(e.getAvatar())
                .remark(e.getRemark())
                .build();
    }

    /** 实体列表 → VO 列表（关联字段由 Service 补齐） */
    public static List<EmployeeVO> toVOList(List<Employee> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(EmployeeConverter::toVO).collect(Collectors.toList());
    }

    /** 直属主管 ID 归一：0 视为"清空" */
    private static Long normalizeManagerId(Long managerId) {
        return (managerId == null || managerId == 0L) ? null : managerId;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
