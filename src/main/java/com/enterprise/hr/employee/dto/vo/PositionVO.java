package com.enterprise.hr.employee.dto.vo;

import com.enterprise.hr.employee.entity.Position;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 岗位 VO（{@code GET /positions}）。
 *
 * <p>除岗位主数据外补两个前端展示字段：
 * <ul>
 *     <li>{@code dept_name} —— 所属部门名，{@code dept_id} 为空时输出「通用」（公司通用岗位）</li>
 *     <li>{@code employeeCount} —— 在编人数，前端 {@code dept/positions.vue} 读 {@code row.employeeCount}，
 *         必须保持驼峰</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PositionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 岗位编码，如 P-TECH-001 */
    private String positionCode;

    /** 岗位名称，如 Java开发工程师 */
    private String positionName;

    /** 岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作 */
    private Integer positionSeq;

    /** 职级范围，如 P4-P7 */
    private String jobLevel;

    /** 所属部门 ID（null = 公司通用岗位） */
    private Long deptId;

    /** 所属部门名（deptId 为空时为「通用」） */
    private String deptName;

    /** 岗位职责 */
    private String duty;

    /** 任职要求 */
    private String requirement;

    /** 岗位编制上限（0 = 不限） */
    private Integer headcountLimit;

    /** 显示排序 */
    private Integer sortNo;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 在编人数（status != 0 的员工数） */
    @JsonProperty("employeeCount")
    private Long employeeCount;

    /** 实体 → VO（deptName / employeeCount 由 Service 批量补齐） */
    public static PositionVO from(Position position) {
        if (position == null) {
            return null;
        }
        PositionVO vo = new PositionVO();
        vo.setId(position.getId());
        vo.setPositionCode(position.getPositionCode());
        vo.setPositionName(position.getPositionName());
        vo.setPositionSeq(position.getPositionSeq());
        vo.setJobLevel(position.getJobLevel());
        vo.setDeptId(position.getDeptId());
        vo.setDuty(position.getDuty());
        vo.setRequirement(position.getRequirement());
        vo.setHeadcountLimit(position.getHeadcountLimit());
        vo.setSortNo(position.getSortNo());
        vo.setStatus(position.getStatus());
        return vo;
    }
}
