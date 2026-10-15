package com.enterprise.hr.system.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.system.dto.vo.SysDictItemVO;
import com.enterprise.hr.system.dto.vo.SysDictTypeVO;
import com.enterprise.hr.system.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据字典接口（F-09-04）。
 *
 * <p>不加 {@code @RequireRoles}：字典是只读的枚举真源，所有已登录用户的前端页面
 * （员工表单、考勤筛选、绩效评级……）都依赖它渲染下拉框，限制角色会直接让页面白屏。
 * 字典内容本身不含任何业务敏感数据。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "系统管理-数据字典", description = "字典类型 / 字典项")
@RestController
@RequestMapping("/sys/dict-types")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    /** 字典类型列表 */
    @Operation(summary = "字典类型列表")
    @GetMapping
    public R<PageResult<SysDictTypeVO>> listTypes() {
        return R.ok(sysDictService.listTypes());
    }

    /** 指定字典类型下的字典项（按 sort_no 升序） */
    @Operation(summary = "字典项列表")
    @GetMapping("/{typeId}/items")
    public R<PageResult<SysDictItemVO>> listItems(@PathVariable Long typeId) {
        return R.ok(sysDictService.listItems(typeId));
    }
}
