package com.enterprise.hr.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新建公告请求。
 *
 * <p>请求体字段一律<b>驼峰</b>（与前端 {@code system} 模块 npm 约定一致），
 * 而响应 VO 用蛇形，两者互不影响。
 *
 * @author Enterprise HR Team
 */
@Data
public class NoticeCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公告标题 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题长度不得超过 200")
    private String title;

    /** 公告正文 */
    private String content;

    /** 公告类型：1 公司制度 / 2 放假通知 / 3 人事公告 / 4 活动通知 / 5 其他；缺省 3 人事公告 */
    private Integer noticeType;

    /**
     * 发布范围：1 全体 / 2 指定部门；缺省 1 全体。
     * 为 2 时必须同时提供 {@link #targetDeptIds}。
     */
    private Integer targetScope;

    /** 指定部门 ID 列表，逗号分隔（targetScope = 2 时必填） */
    private String targetDeptIds;

    /** 是否置顶：1 是 / 0 否；缺省 0 */
    private Integer isTop;

    /** 失效时间，到期由系统自动下架；为空表示长期有效 */
    private LocalDateTime expireTime;

    /**
     * 是否直接发布。默认 false —— 新建即落为草稿。
     * 公告一旦发布就是全员可见，必须留下"确认发布"这一显式动作。
     */
    private Boolean publishNow;
}
