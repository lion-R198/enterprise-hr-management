package com.enterprise.hr.recruit.entity;

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
 * JobPosting实体（对应表 {@code job_posting}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("job_posting")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class JobPosting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 发布渠道:1官网 2智联招聘 3前程无忧 4BOSS直聘 5猎聘 6内部推荐 7其他 */
    private Integer channel;

    /** 发布职位名称 */
    private String postingTitle;

    /** 发布链接 */
    private String postingUrl;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 浏览量 */
    private Integer viewCount;

    /** 投递量 */
    private Integer applyCount;

    /** 状态:1发布中 2已关闭 */
    private Integer status;

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
