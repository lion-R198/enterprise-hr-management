package com.enterprise.hr.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页响应体。
 *
 * <p>字段与前端 {@code src/utils/request.js} 消费的分页结构严格一致：
 * {@code { records, total, current, size }}，前端表格统一读取 {@code records} / {@code total}。
 *
 * <p>为什么不直接返回 MyBatis-Plus 的 {@code IPage}：
 * {@code IPage} 会额外序列化 {@code pages/orders/optimizeCountSql/searchCount/countId}
 * 等内部字段，既污染接口契约也暴露实现细节。统一收敛为四字段，接口风格一致。
 *
 * @param <T> 记录类型
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private Long total;
    /** 当前页码，从 1 开始 */
    private Long current;
    /** 每页条数 */
    private Long size;

    /** 空页 */
    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, current, size);
    }

    /** 由 MyBatis-Plus 分页对象转换（记录类型不变） */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 由 MyBatis-Plus 分页对象转换，并把实体映射为目标 VO */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> list = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 由内存列表构造分页（用于已全量加载后过滤的场景，如统计/字典等小数据集）。
     * 语义与数据库分页保持一致，避免前端出现两种分页行为。
     */
    public static <T> PageResult<T> of(List<T> list, long current, long size) {
        List<T> src = list == null ? Collections.emptyList() : list;
        long pageNo = current < 1 ? 1L : current;
        long pageSize = size < 1 ? 10L : size;
        int from = (int) Math.min((long) src.size(), (pageNo - 1) * pageSize);
        int to = (int) Math.min((long) src.size(), from + pageSize);
        return new PageResult<>(src.subList(from, to), (long) src.size(), pageNo, pageSize);
    }

    /** 非分页的列表响应（前端"我的 xxx"类接口读 records / total） */
    public static <T> PageResult<T> list(List<T> list) {
        List<T> src = list == null ? Collections.emptyList() : list;
        return new PageResult<>(src, (long) src.size(), 1L, (long) src.size());
    }
}
