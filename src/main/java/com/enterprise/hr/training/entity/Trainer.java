package com.enterprise.hr.training.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trainer实体（对应表 {@code trainer}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("trainer")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Trainer implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 讲师编号(唯一) */
    private String trainerCode;

    /** 讲师姓名 */
    private String trainerName;

    /** 讲师类型:1内部讲师 2外部讲师 */
    private Integer trainerType;

    /** 内部讲师关联员工ID(外部讲师为空) */
    private Long employeeId;

    /** 擅长领域,如 Java架构/项目管理 */
    private String expertise;

    /** 讲师等级:1初级 2中级 3高级 4资深 */
    private Integer trainerLevel;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 所属机构(外部讲师) */
    private String orgName;

    /** 课时费(元/小时) */
    private BigDecimal hourRate;

    /** 累计授课次数 */
    private Integer teachCount;

    /** 平均评分(5分制) */
    private BigDecimal avgScore;

    /** 状态:1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
