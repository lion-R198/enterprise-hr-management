package com.enterprise.hr.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 字段自动填充处理器：为 {@code createdAt} / {@code updatedAt} 赋值。
 *
 * <p>配合实体上的 {@code @TableField(fill = ...)} 生效：
 * <ul>
 *     <li>INSERT 时填 {@code createdAt} 与 {@code updatedAt}</li>
 *     <li>UPDATE 时只填 {@code updatedAt}，{@code createdAt} 保持不变</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@Component
public class MybatisAutoFillHandler implements MetaObjectHandler {

    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, FIELD_CREATED_AT, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, FIELD_UPDATED_AT, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 只更新 updatedAt：createdAt 语义上是"创建时间"，任何修改都不应改变它
        this.strictUpdateFill(metaObject, FIELD_UPDATED_AT, LocalDateTime.class, LocalDateTime.now());
    }
}
