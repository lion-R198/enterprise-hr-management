package com.enterprise.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典项实体（对应表 {@code sys_dict_item}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_dict_item")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属字典类型ID（sys_dict_type.id） */
    @TableField("dict_type_id")
    private Long dictTypeId;

    /** 字典项值（存库值），如 "3" */
    @TableField("item_value")
    private String itemValue;

    /** 字典项显示文本，如 "本科" */
    @TableField("item_label")
    private String itemLabel;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 是否默认选中：1 是 / 0 否 */
    @TableField("is_default")
    private Integer isDefault;

    /** 前端样式类（标签颜色等） */
    @TableField("css_class")
    private String cssClass;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
