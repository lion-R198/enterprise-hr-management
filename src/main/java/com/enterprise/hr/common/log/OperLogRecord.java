package com.enterprise.hr.common.log;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志采集载体：切面与落库实现之间的数据传输对象。
 *
 * <p>字段与 {@code sys_oper_log} 列一一对应，落库实现只做字段映射，不含业务判断。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
public class OperLogRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模块名称 */
    private String module;
    /** 操作描述 */
    private String operDesc;
    /** 操作类型（枚举名，落库时映射为数值） */
    private OperType operType;
    /** 请求方法：GET/POST/PUT/DELETE */
    private String requestMethod;
    /** 请求 URL */
    private String requestUrl;
    /** 请求参数（仅在 saveParams=true 时填充） */
    private String requestParam;
    /** 操作人用户ID */
    private Long operUserId;
    /** 操作人姓名（快照，避免用户改名后历史日志失真） */
    private String operUserName;
    /** 操作人IP */
    private String operIp;
    /** 结果状态：1 成功 / 0 失败 */
    private Integer operStatus;
    /** 结果提示信息（失败时为错误原因） */
    private String resultMessage;
    /** 耗时（毫秒） */
    private Long costTime;
}
