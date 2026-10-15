package com.enterprise.hr.notice.entity;

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
 * Notice实体（对应表 {@code notice}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notice")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告正文 */
    private String content;

    /** 公告类型:1公司制度 2放假通知 3人事公告 4活动通知 5其他 */
    private Integer noticeType;

    /** 发布人员工ID */
    private Long publisherId;

    /** 发布时间(支持定时发布) */
    private LocalDateTime publishTime;

    /** 失效时间(到期自动下架) */
    private LocalDateTime expireTime;

    /** 发布范围:1全体 2指定部门 */
    private Integer targetScope;

    /** 指定部门ID列表,逗号分隔 */
    private String targetDeptIds;

    /** 是否置顶:1是 0否 */
    private Integer isTop;

    /** 已读人数 */
    private Integer readCount;

    /** 状态:1草稿 2已发布 3已下架 */
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
