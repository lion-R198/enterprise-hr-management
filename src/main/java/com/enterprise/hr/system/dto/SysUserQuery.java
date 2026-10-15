package com.enterprise.hr.system.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户分页查询条件。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 关键字：登录账号或姓名模糊匹配 */
    private String keyword;

    /** 账号状态：1 正常 / 0 停用 / 2 锁定 */
    private Integer status;
}
