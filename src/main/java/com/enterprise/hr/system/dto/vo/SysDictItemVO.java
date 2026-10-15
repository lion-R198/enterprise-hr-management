package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典项 VO。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysDictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 所属字典类型 ID */
    private Long dictTypeId;
    /** 字典项值（存库值），如 3 */
    private String itemValue;
    /** 字典项显示文本，如 本科 */
    private String itemLabel;
    /** 显示排序，升序 */
    private Integer sortNo;
    /** 是否默认选中：1 是 / 0 否 */
    private Integer isDefault;
    /** 前端样式类（标签颜色等） */
    private String cssClass;
    /** 状态：1 启用 / 0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
