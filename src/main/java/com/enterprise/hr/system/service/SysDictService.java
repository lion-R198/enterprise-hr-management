package com.enterprise.hr.system.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.system.dto.vo.SysDictItemVO;
import com.enterprise.hr.system.dto.vo.SysDictTypeVO;

/**
 * 数据字典服务（F-09-04）。
 *
 * <p>字典是"枚举值的唯一真源"：前端页面上所有下拉框（性别、学历、假别、数据范围……）
 * 都应从这里取，而不是在页面里硬编码 option，否则新增一个假别就要改前端发版。
 *
 * <p>字典属于"小数据集"（当前 5 类、数十项），不做真正的分页：一次全量返回，
 * 响应结构仍用统一的 {@link PageResult}，使前端可以用同一套 {@code records} 渲染逻辑。
 *
 * @author Enterprise HR Team
 */
public interface SysDictService {

    /** 全部字典类型（按 id 升序） */
    PageResult<SysDictTypeVO> listTypes();

    /** 指定字典类型下的字典项，按 {@code sort_no} 升序 */
    PageResult<SysDictItemVO> listItems(Long typeId);
}
