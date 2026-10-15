package com.enterprise.hr.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.notice.dto.NoticeCreateReq;
import com.enterprise.hr.notice.dto.NoticeQuery;
import com.enterprise.hr.notice.dto.vo.NoticeVO;
import com.enterprise.hr.notice.entity.Notice;
import com.enterprise.hr.notice.mapper.NoticeMapper;
import com.enterprise.hr.notice.service.NoticeService;
import com.enterprise.hr.system.dto.vo.OpResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知公告服务实现。
 *
 * @author Enterprise HR Team
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    /** 公告类型取值下界：1 公司制度 */
    private static final int NOTICE_TYPE_MIN = BizConst.NOTICE_TYPE_POLICY;
    /** 公告类型取值上界：5 其他 */
    private static final int NOTICE_TYPE_MAX = BizConst.NOTICE_TYPE_MAX;
    /** 发布范围为「指定部门」 */
    private static final int SCOPE_DEPTS = BizConst.NOTICE_SCOPE_DEPTS;
    /** 发布范围为「全体」 */
    private static final int SCOPE_ALL = BizConst.NOTICE_SCOPE_ALL;

    private final NoticeMapper noticeMapper;
    private final EmployeeMapper employeeMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper, EmployeeMapper employeeMapper) {
        this.noticeMapper = noticeMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public PageResult<NoticeVO> page(NoticeQuery query) {
        LambdaQueryWrapper<Notice> wrapper = Wrappers.<Notice>lambdaQuery()
                .eq(query.getStatus() != null, Notice::getStatus, query.getStatus())
                .eq(query.getNoticeType() != null, Notice::getNoticeType, query.getNoticeType())
                .like(StringUtils.hasText(query.getKeyword()), Notice::getTitle, trim(query.getKeyword()))
                // 置顶优先，其后按发布时间倒序；草稿没有发布时间，用创建顺序兜底（id 倒序）
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId);

        IPage<Notice> page = noticeMapper.selectPage(query.toPage(), wrapper);
        List<Notice> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }
        Map<Long, String> publisherNames = loadPublisherNames(records);
        return PageResult.of(page, notice -> toVO(notice, publisherNames));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO create(NoticeCreateReq req) {
        int noticeType = req.getNoticeType() == null ? BizConst.NOTICE_TYPE_HR : req.getNoticeType();        if (noticeType < NOTICE_TYPE_MIN || noticeType > NOTICE_TYPE_MAX) {
            throw BizException.badRequest("公告类型仅支持 1~5（公司制度/放假通知/人事公告/活动通知/其他）");
        }
        int targetScope = req.getTargetScope() == null ? SCOPE_ALL : req.getTargetScope();
        if (targetScope != SCOPE_ALL && targetScope != SCOPE_DEPTS) {
            throw BizException.badRequest("发布范围仅支持 1（全体）或 2（指定部门）");
        }
        // 「指定部门」却不给部门 = 一份谁都收不到的公告，必须当场拒绝
        if (targetScope == SCOPE_DEPTS && !StringUtils.hasText(req.getTargetDeptIds())) {
            throw BizException.badRequest("发布范围选择「指定部门」时必须指定接收部门");
        }

        boolean publishNow = Boolean.TRUE.equals(req.getPublishNow());
        Notice entity = new Notice();
        entity.setTitle(req.getTitle().trim());
        entity.setContent(req.getContent());
        entity.setNoticeType(noticeType);
        // publisher_id 指向 employee.id：发布人可能没有系统账号（如部门主管代发），
        // 因此这里取当前登录用户绑定的员工 ID，未绑定则为空
        entity.setPublisherId(UserContext.currentEmployeeId());
        entity.setTargetScope(targetScope);
        entity.setTargetDeptIds(targetScope == SCOPE_DEPTS ? req.getTargetDeptIds().trim() : null);
        entity.setIsTop(req.getIsTop() == null ? BizConst.FLAG_NO : req.getIsTop());
        entity.setExpireTime(req.getExpireTime());
        entity.setReadCount(0);
        if (publishNow) {
            entity.setStatus(BizConst.NOTICE_PUBLISHED);
            entity.setPublishTime(LocalDateTime.now());
        } else {
            entity.setStatus(BizConst.NOTICE_DRAFT);
            entity.setPublishTime(null);
        }
        noticeMapper.insert(entity);
        return OpResultVO.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO publish(Long id) {
        Notice notice = requireNotice(id);
        if (Objects.equals(notice.getStatus(), BizConst.NOTICE_PUBLISHED)) {
            // 重复发布会让发布时间被刷新，公告又一次"跳到最新"，对全员形成骚扰
            throw BizException.badRequest("公告已发布，无需重复发布");
        }
        noticeMapper.update(null, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getId, id)
                .set(Notice::getStatus, BizConst.NOTICE_PUBLISHED)
                .set(Notice::getPublishTime, LocalDateTime.now()));
        return OpResultVO.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpResultVO offline(Long id) {
        Notice notice = requireNotice(id);
        if (!Objects.equals(notice.getStatus(), BizConst.NOTICE_PUBLISHED)) {
            // 草稿从未发布、已下架无需再下架：明确报错而不是"静默成功"，
            // 否则前端会提示"已下架"，与用户看到的实际状态不符
            throw BizException.badRequest("仅已发布的公告可以下架");
        }
        noticeMapper.update(null, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getId, id)
                .set(Notice::getStatus, BizConst.NOTICE_OFFLINE));
        return OpResultVO.ok();
    }

    // ------------------------------------------------------------------ 内部

    private Notice requireNotice(Long id) {
        Notice notice = id == null ? null : noticeMapper.selectById(id);
        if (notice == null) {
            throw BizException.notFound("公告不存在");
        }
        return notice;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 一次 IN 查询补齐发布人姓名，避免逐条查员工表（N+1） */
    private Map<Long, String> loadPublisherNames(List<Notice> records) {
        List<Long> ids = records.stream().map(Notice::getPublisherId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectList(Wrappers.<Employee>lambdaQuery().in(Employee::getId, ids)).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private NoticeVO toVO(Notice notice, Map<Long, String> publisherNames) {
        NoticeVO vo = new NoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setNoticeType(notice.getNoticeType());
        vo.setPublisherId(notice.getPublisherId());
        vo.setPublisherName(notice.getPublisherId() == null ? null : publisherNames.get(notice.getPublisherId()));
        vo.setPublishTime(notice.getPublishTime());
        vo.setExpireTime(notice.getExpireTime());
        vo.setTargetScope(notice.getTargetScope());
        vo.setTargetDeptIds(notice.getTargetDeptIds());
        vo.setIsTop(notice.getIsTop());
        vo.setReadCount(notice.getReadCount());
        vo.setStatus(notice.getStatus());
        return vo;
    }
}
