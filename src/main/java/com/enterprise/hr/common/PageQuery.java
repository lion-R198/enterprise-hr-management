package com.enterprise.hr.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一分页查询入参基类。
 *
 * <p>所有分页接口的查询 DTO 继承本类，保证 {@code current} / {@code size} 参数名与语义全局一致
 * （前端 {@code el-pagination} 直接透传，无需逐页适配）。
 *
 * @author Enterprise HR Team
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认页大小 */
    public static final long DEFAULT_SIZE = 10L;
    /** 单页上限，防止前端传入超大 size 拖垮数据库（与 PaginationInnerInterceptor.maxLimit 一致） */
    public static final long MAX_SIZE = 500L;

    /** 当前页码，从 1 开始 */
    @Min(value = 1, message = "页码不得小于 1")
    private Long current = 1L;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数不得小于 1")
    @Max(value = MAX_SIZE, message = "每页条数不得超过 500")
    private Long size = DEFAULT_SIZE;

    /** 归一化后的页码（空值/非法值兜底为 1） */
    public long safeCurrent() {
        return (current == null || current < 1) ? 1L : current;
    }

    /** 归一化后的页大小（空值/非法值兜底为默认值，并做上限截断） */
    public long safeSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** 构造 MyBatis-Plus 分页对象 */
    public <T> Page<T> toPage() {
        return new Page<>(safeCurrent(), safeSize());
    }
}
