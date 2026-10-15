package com.enterprise.hr.employee.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增 / 修改入参（{@code POST /depts}、{@code PUT /depts/{id}}）。
 *
 * <p>同一个 DTO 服务两个接口：新增要求 {@code name} 与 {@code deptCode} 必填，
 * 修改是<b>局部更新</b>（未传字段保持原值，允许只改电话/编制），
 * 因此必填校验不放 {@code @NotBlank}，而在 Service 内按场景校验
 * —— 否则"改个电话"也会因为没传 name 被 400 拒绝。
 *
 * @author Enterprise HR Team
 */
@Data
public class DeptSaveReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门编码：新增必填、全局唯一（BR-03-01） */
    private String deptCode;

    /** 部门名称：新增必填、同级不可重复（BR-03-01） */
    private String name;

    /** 上级部门 ID（null = 顶级部门）；修改时可用来调整挂靠位置 */
    private Long parentId;

    /** 组织类型：1 公司 / 2 中心 / 3 部门 / 4 班组，默认 3 */
    private Integer deptType;

    /** 编制上限（0 = 不限） */
    private Integer headcountLimit;

    /** 部门联系电话 */
    private String phone;

    /** 显示排序 */
    private Integer sortNo;

    /** 部门负责人员工 ID */
    private Long leaderId;

    /** 部门负责人姓名（快照） */
    private String leader;

    /** 状态：1 正常 / 0 停用 */
    private Integer status;
}
