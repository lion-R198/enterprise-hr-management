package com.enterprise.hr.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 修改系统用户请求（局部更新，未传字段保持原值）。
 *
 * <p>按前端 {@code system/user.vue} 的实际用法设计：
 * <ul>
 *     <li>「停用/启用」只传 {@code status}</li>
 *     <li>「编辑」传 {@code realName} + {@code roles}</li>
 * </ul>
 * 因此所有字段均可选。
 *
 * @author Enterprise HR Team
 */
@Data
public class SysUserUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 姓名 */
    private String realName;

    /** 账号状态：1 正常 / 0 停用 */
    private Integer status;

    /** 角色编码集合（全量覆盖语义） */
    private List<String> roles;

    /** 关联员工 ID */
    private Long employeeId;

    /** 所属部门 ID */
    private Long deptId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
