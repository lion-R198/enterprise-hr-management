package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志 VO。
 *
 * <p>列表页只消费其中的时间、操作人、模块、类型、描述、请求地址、耗时与结果；
 * 参数与响应体（{@code request_param} / {@code response_result}）体量大且可能含敏感信息，
 * 列表场景下按需返回更合适，故本 VO <b>不包含</b>它们。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysOperLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 操作人账号 */
    private String username;
    /** 所属模块，如「员工信息」 */
    private String module;
    /** 操作类型：1 新增 2 修改 3 删除 4 查询 5 导出 6 导入 7 其他 */
    private Integer operType;
    /** 操作描述，如「新增员工档案」 */
    private String operDesc;
    /** HTTP 方法 */
    private String requestMethod;
    /** 请求地址 */
    private String requestUrl;
    /** 操作 IP */
    private String operIp;
    /** 操作状态：1 成功 / 0 失败 */
    private Integer operStatus;
    /** 异常信息（失败时） */
    private String errorMsg;
    /** 耗时（毫秒） */
    private Long costTime;
    /** 操作时间 */
    private LocalDateTime operTime;
}
