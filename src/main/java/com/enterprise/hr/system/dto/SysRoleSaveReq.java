package com.enterprise.hr.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色新增/修改请求。
 *
 * @author Enterprise HR Team
 */
@Data
public class SysRoleSaveReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色编码，如 ROLE_AUDIT（唯一；系统内置角色不可修改） */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不得超过 64")
    private String roleCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不得超过 64")
    private String roleName;

    /** 数据范围：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 / 5 自定义 */
    private Integer dataScope;

    /** 自定义数据范围部门 ID 列表，逗号分隔（dataScope = 5 时有效） */
    private String dataScopeDepts;

    /** 职责描述 */
    private String description;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
