package com.enterprise.hr.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统参数查询条件。
 *
 * @author Enterprise HR Team
 */
@Data
public class SysConfigQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数分组筛选：attendance / salary / recruit / system */
    private String group;
}
