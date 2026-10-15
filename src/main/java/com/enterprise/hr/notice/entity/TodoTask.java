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
 * TodoTask实体（对应表 {@code todo_task}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("todo_task")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TodoTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 待办接收人员工ID */
    private Long receiverId;

    /** 待办类型:1待审批 2合同到期 3转正提醒 4绩效评分 5培训报名 6其他 */
    private Integer taskType;

    /** 待办标题 */
    private String taskTitle;

    /** 待办详情 */
    private String taskContent;

    /** 业务类型(同approval_record.biz_type) */
    private Integer bizType;

    /** 业务单据ID(多态,不设外键) */
    private Long bizId;

    /** 业务单号 */
    private String bizNo;

    /** 优先级:1低 2中 3高 */
    private Integer priority;

    /** 处理截止时间 */
    private LocalDateTime deadline;

    /** 处理状态:0未处理 1已处理 2已忽略 */
    private Integer handleStatus;

    /** 处理时间 */
    private LocalDateTime handleTime;

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
