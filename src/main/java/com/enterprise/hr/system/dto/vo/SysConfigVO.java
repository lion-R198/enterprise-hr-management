package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统参数 VO。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 参数键（唯一），如 attendance.lock.day */
    private String configKey;
    /** 参数名称，如 考勤封账日 */
    private String configName;
    /** 参数值 */
    private String configValue;
    /** 参数分组：attendance 考勤 / salary 薪资 / recruit 招聘 / system 系统 */
    private String configGroup;
    /** 值类型：1 字符串 2 数字 3 布尔 4 JSON */
    private Integer valueType;
    /** 是否系统内置：1 是（禁止删除、禁止置空） / 0 否 */
    private Integer isSystem;
    /** 备注说明（通常标注该参数由哪条业务规则消费） */
    private String remark;
}
