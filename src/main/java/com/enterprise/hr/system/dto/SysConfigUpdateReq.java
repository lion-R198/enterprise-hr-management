package com.enterprise.hr.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统参数修改请求。
 *
 * @author Enterprise HR Team
 */
@Data
public class SysConfigUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数值 */
    @NotNull(message = "参数值不能为空")
    private String configValue;
}
