package com.enterprise.hr.employee.dto.vo;

import com.enterprise.hr.employee.entity.Dept;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门树节点 VO（{@code GET /depts/tree} 与 {@code GET /depts} 共用）。
 *
 * <p>字段命名：随类上的蛇形策略输出 {@code dept_code} / {@code dept_type} / {@code headcount_limit}，
 * 前端 {@code dept/index.vue} 的 {@code el-table} 直接以列名绑定。
 * 唯一例外是 {@code employeeCount} —— 前端读 {@code row.employeeCount}，必须保持驼峰。
 *
 * <p>{@code children} 无子节点时<b>不输出该字段</b>：{@code el-table} 的树形渲染以
 * "children 是否存在且非空"判断是否显示展开箭头，输出空数组会出现"可展开但展开后无内容"的假箭头。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeptTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 部门编码，如 D1001 */
    private String deptCode;

    /** 部门名称 */
    private String name;

    /** 上级部门 ID（null = 顶级部门） */
    private Long parentId;

    /** 部门负责人姓名（快照） */
    private String leader;

    /** 部门负责人员工 ID */
    private Long leaderId;

    /** 组织类型：1 公司 / 2 中心 / 3 部门 / 4 班组 */
    private Integer deptType;

    /** 层级深度，顶级 = 1 */
    private Integer deptLevel;

    /** 物化路径，如 /1/2/5/ */
    private String deptPath;

    /** 编制上限（0 = 不限） */
    private Integer headcountLimit;

    /** 部门联系电话 */
    private String phone;

    /** 显示排序 */
    private Integer sortNo;

    /** 状态：1 正常 / 0 停用 */
    private Integer status;

    /** 在岗人数（status != 0）；F-03-04 编制占用率由它 + headcount_limit 计算 */
    @JsonProperty("employeeCount")
    private Long employeeCount;

    /** 子部门；无子节点时不输出（NON_EMPTY） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DeptTreeVO> children;

    /** 实体 → 节点（不处理 children 与 employeeCount，由 Service 补齐） */
    public static DeptTreeVO from(Dept dept) {
        if (dept == null) {
            return null;
        }
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setDeptCode(dept.getDeptCode());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setLeader(dept.getLeader());
        vo.setLeaderId(dept.getLeaderId());
        vo.setDeptType(dept.getDeptType());
        vo.setDeptLevel(dept.getDeptLevel());
        vo.setDeptPath(dept.getDeptPath());
        vo.setHeadcountLimit(dept.getHeadcountLimit());
        vo.setPhone(dept.getPhone());
        vo.setSortNo(dept.getSortNo());
        vo.setStatus(dept.getStatus());
        return vo;
    }
}
