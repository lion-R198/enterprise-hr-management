package com.enterprise.hr.employee.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 岗位新增 / 修改入参（{@code POST /positions}、{@code PUT /positions/{id}}）。
 *
 * <p>新增要求 {@code positionCode} 与 {@code positionName} 必填，
 * 修改为局部更新（未传字段保持原值），故必填校验在 Service 内按场景进行。
 *
 * @author Enterprise HR Team
 */
@Data
public class PositionSaveReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 岗位编码：新增必填、全局唯一，如 P-TECH-001 */
    private String positionCode;

    /** 岗位名称：新增必填，如 Java开发工程师 */
    private String positionName;

    /** 岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作，默认 2 */
    private Integer positionSeq;

    /** 职级范围，如 P4-P7 */
    private String jobLevel;

    /** 所属部门 ID（null = 公司通用岗位） */
    private Long deptId;

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

    /** 备注 */
    private String remark;
}
