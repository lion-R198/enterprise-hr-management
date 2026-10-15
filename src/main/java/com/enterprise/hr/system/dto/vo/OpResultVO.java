package com.enterprise.hr.system.dto.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * 写操作通用结果：{@code {success, id}}。
 *
 * <p>前端约定：新增类接口返回新记录主键，便于创建后定位/跳转；
 * 仅返回 {@code {success:true}} 会让前端"创建成功但不知道创了哪条"。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success = Boolean.TRUE;
    /** 新建记录主键 */
    private Long id;

    /**
     * 重置后的初始密码，仅"重置密码"接口返回。
     * 前端 {@code system/user.vue} 提示语中引用的是固定文案，
     * 此处保留该字段（驼峰）便于后续改为服务端下发时前端无需改动。
     */
    @JsonProperty("initPassword")
    private String initPassword;

    /** 成功（无主键） */
    public static OpResultVO ok() {
        return new OpResultVO();
    }

    /** 成功并返回主键 */
    public static OpResultVO ok(Long id) {
        OpResultVO vo = new OpResultVO();
        vo.setId(id);
        return vo;
    }

    /** 成功并返回初始密码 */
    public static OpResultVO withInitPassword(String initPassword) {
        OpResultVO vo = new OpResultVO();
        vo.setInitPassword(initPassword);
        return vo;
    }
}
