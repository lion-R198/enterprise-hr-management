package com.enterprise.hr.system.entity;

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
 * SysOperLog实体（对应表 {@code sys_oper_log}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oper_log")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 操作人用户ID */
    private Long userId;

    /** 操作人账号 */
    private String username;

    /** 所属模块,如 员工信息 */
    private String module;

    /** 操作类型:1新增 2修改 3删除 4查询 5导出 6导入 7其他 */
    private Integer operType;

    /** 操作描述 */
    private String operDesc;

    /** HTTP方法:GET/POST/PUT/DELETE */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 请求参数(JSON) */
    private String requestParam;

    /** 响应结果(截断存储) */
    private String responseResult;

    /** 操作IP */
    private String operIp;

    /** 操作状态:1成功 0失败 */
    private Integer operStatus;

    /** 异常信息 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
