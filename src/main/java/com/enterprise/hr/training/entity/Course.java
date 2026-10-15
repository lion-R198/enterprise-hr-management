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
 * Course实体（对应表 {@code course}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("course")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程编码(唯一) */
    private String courseCode;

    /** 课程名称 */
    private String courseName;

    /** 授课形式:1线上 2线下 */
    private Integer courseType;

    /** 课程分类,如 技术类/管理类 */
    private String category;

    /** 课程学时 */
    private BigDecimal hours;

    /** 主讲讲师ID */
    private Long trainerId;

    /** 课件下载地址 */
    private String coursewareUrl;

    /** 录播视频地址 */
    private String videoUrl;

    /** 课程大纲 */
    private String outline;

    /** 人数上限(0=不限) */
    private Integer maxStudents;

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
