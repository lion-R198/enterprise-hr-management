package com.enterprise.hr.notice.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.notice.dto.NoticeCreateReq;
import com.enterprise.hr.notice.dto.NoticeQuery;
import com.enterprise.hr.notice.dto.vo.NoticeVO;
import com.enterprise.hr.notice.service.NoticeService;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知公告管理接口（F-01-03 管理端）。
 *
 * <p>与 {@code /dashboard/notices} 的分工：
 * <ul>
 *     <li>{@code /dashboard/notices} —— 员工视角，只返回「已发布且在有效期内」的公告</li>
 *     <li>{@code /notices/**} —— 管理视角，含草稿与已下架，可创建与发布</li>
 * </ul>
 *
 * <p>权限：公告面向全员，属人事职权范围，因此开放给 {@link RoleCode#HR} 与管理员。
 *
 * @author Enterprise HR Team
 */
@Tag(name = "通知公告", description = "公告分页 / 新建 / 发布 / 下架")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@RequireRoles({RoleCode.ADMIN, RoleCode.HR})
public class NoticeController {

    private final NoticeService noticeService;

    /** 公告分页（管理端，含草稿与已下架） */
    @Operation(summary = "公告分页")
    @GetMapping("/page")
    public R<PageResult<NoticeVO>> page(@Valid NoticeQuery query) {
        return R.ok(noticeService.page(query));
    }

    /** 新建公告（默认草稿，{@code publishNow=true} 时直接发布） */
    @Operation(summary = "新建公告")
    @PostMapping
    @OperLog(module = "通知公告", action = "新建公告", type = OperType.INSERT)
    public R<OpResultVO> create(@Valid @RequestBody NoticeCreateReq req) {
        return R.ok(noticeService.create(req));
    }

    /** 发布公告 */
    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    @OperLog(module = "通知公告", action = "发布公告", type = OperType.UPDATE)
    public R<OpResultVO> publish(@PathVariable Long id) {
        return R.ok(noticeService.publish(id));
    }

    /** 下架公告 */
    @Operation(summary = "下架公告")
    @PutMapping("/{id}/offline")
    @OperLog(module = "通知公告", action = "下架公告", type = OperType.UPDATE)
    public R<OpResultVO> offline(@PathVariable Long id) {
        return R.ok(noticeService.offline(id));
    }
}
