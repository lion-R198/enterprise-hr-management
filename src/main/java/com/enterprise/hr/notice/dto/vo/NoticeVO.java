package com.enterprise.hr.notice.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告 VO。
 *
 * <p>相对实体补充了 {@code publisherName}：公告列表需要展示"谁发的"，
 * 只给 {@code publisher_id} 前端还得再查一次员工接口。
 *
 * <p>注意 {@code publisherId} 是<b>员工 ID</b>（{@code notice.publisher_id → employee.id}），
 * 不是系统用户 ID——公告面向全体员工，发布人不一定有系统账号。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 公告标题 */
    private String title;
    /** 公告正文 */
    private String content;
    /** 公告类型：1 公司制度 / 2 放假通知 / 3 人事公告 / 4 活动通知 / 5 其他 */
    private Integer noticeType;
    /** 发布人员工 ID */
    private Long publisherId;
    /** 发布人姓名 */
    private String publisherName;
    /** 发布时间（草稿为空） */
    private LocalDateTime publishTime;
    /** 失效时间 */
    private LocalDateTime expireTime;
    /** 发布范围：1 全体 / 2 指定部门 */
    private Integer targetScope;
    /** 指定部门 ID 列表，逗号分隔 */
    private String targetDeptIds;
    /** 是否置顶：1 是 / 0 否 */
    private Integer isTop;
    /** 已读人数 */
    private Integer readCount;
    /** 状态：1 草稿 / 2 已发布 / 3 已下架 */
    private Integer status;
}
