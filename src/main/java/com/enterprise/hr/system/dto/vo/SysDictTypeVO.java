package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型 VO。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysDictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 字典类型编码（唯一），如 education / gender / leave_type */
    private String dictType;
    /** 字典类型名称，如 最高学历 */
    private String dictName;
    /** 状态：1 启用 / 0 停用 */
    private Integer status;
    /** 备注（通常标注该字典被哪张表的哪个字段使用） */
    private String remark;
}
