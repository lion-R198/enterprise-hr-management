package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色列表 VO。
 *
 * <p>不直接返回 {@code SysRole} 实体：实体带有 {@code deleted} 逻辑删除标志与
 * 审计时间戳，属于实现细节，不应出现在接口契约里。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 角色编码，如 ROLE_HR */
    private String roleCode;
    /** 角色名称，如 人事专员 */
    private String roleName;
    /** 职责描述 */
    private String description;
    /** 数据范围：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 / 5 自定义 */
    private Integer dataScope;
    /** 自定义数据范围部门 ID 列表，逗号分隔 */
    private String dataScopeDepts;
    /** 角色类型：1 系统内置 / 2 自定义 */
    private Integer roleType;
    /** 显示排序 */
    private Integer sortNo;
    /** 状态：1 启用 / 0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
