package com.enterprise.hr.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * NoticeRead实体（对应表 {@code notice_read}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notice_read")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NoticeRead implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 公告ID */
    private Long noticeId;

    /** 阅读人员工ID */
    private Long employeeId;

    /** 阅读时间 */
    private LocalDateTime readTime;
}
