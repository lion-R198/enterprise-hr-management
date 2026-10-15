package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.dto.PositionQuery;
import com.enterprise.hr.employee.dto.PositionSaveReq;
import com.enterprise.hr.employee.dto.vo.PositionVO;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.employee.service.PositionService;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位业务实现。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    /** 公司通用岗位的展示名（position.dept_id 为空） */
    private static final String COMMON_DEPT_NAME = "通用";
    /** 默认岗位序列：2 技术 */
    private static final int DEFAULT_POSITION_SEQ = 2;

    private final EmployeeInfoSupport employeeInfoSupport;

    /** 岗位分页：补部门名与在编人数（两次批量查询覆盖整页） */
    @Override
    public PageResult<PositionVO> page(PositionQuery query) {
        PositionQuery q = query == null ? new PositionQuery() : query;

        LambdaQueryWrapper<Position> wrapper = Wrappers.<Position>lambdaQuery();
        if (q.getDeptId() != null) {
            wrapper.eq(Position::getDeptId, q.getDeptId());
        }
        if (q.getStatus() != null) {
            wrapper.eq(Position::getStatus, q.getStatus());
        }
        wrapper.orderByAsc(Position::getSortNo).orderByAsc(Position::getId);

        IPage<Position> page = baseMapper.selectPage(q.toPage(), wrapper);
        List<PositionVO> vos = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        if (page.getRecords() != null) {
            for (Position position : page.getRecords()) {
                PositionVO vo = PositionVO.from(position);
                vos.add(vo);
                deptIds.add(vo.getDeptId());
            }
        }
        Map<Long, String> deptNames = employeeInfoSupport.loadDeptNames(deptIds);
        Map<Long, Long> counts = employeeInfoSupport.countByPosition();
        for (PositionVO vo : vos) {
            vo.setDeptName(vo.getDeptId() == null ? COMMON_DEPT_NAME : deptNames.get(vo.getDeptId()));
            vo.setEmployeeCount(counts.getOrDefault(vo.getId(), 0L));
        }
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PositionSaveReq req) {
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        String positionCode = trim(req.getPositionCode());
        String positionName = trim(req.getPositionName());
        if (!StringUtils.hasText(positionCode)) {
            throw BizException.badRequest("缺少必填字段：positionCode");
        }
        if (!StringUtils.hasText(positionName)) {
            throw BizException.badRequest("缺少必填字段：positionName");
        }
        Long duplicated = baseMapper.selectCount(
                Wrappers.<Position>lambdaQuery().eq(Position::getPositionCode, positionCode));
        if (duplicated != null && duplicated > 0) {
            throw BizException.badRequest("岗位编码已存在: " + positionCode);
        }

        Position entity = new Position();
        entity.setPositionCode(positionCode);
        entity.setPositionName(positionName);
        entity.setPositionSeq(req.getPositionSeq() == null ? DEFAULT_POSITION_SEQ : req.getPositionSeq());
        entity.setJobLevel(trim(req.getJobLevel()));
        entity.setDeptId(req.getDeptId());
        entity.setDuty(trim(req.getDuty()));
        entity.setRequirement(trim(req.getRequirement()));
        entity.setHeadcountLimit(req.getHeadcountLimit() == null ? 0 : req.getHeadcountLimit());
        entity.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        entity.setStatus(req.getStatus() == null ? BizConst.STATUS_ENABLED : req.getStatus());
        entity.setRemark(trim(req.getRemark()));
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PositionSaveReq req) {
        if (id == null) {
            throw BizException.badRequest("缺少必填参数：id");
        }
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        Position existed = baseMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在: " + id);
        }

        // 岗位编码是外部引用主键，不允许修改（positionCode 传了也不生效）
        if (StringUtils.hasText(req.getPositionName())) {
            existed.setPositionName(req.getPositionName().trim());
        }
        if (req.getJobLevel() != null) {
            existed.setJobLevel(trim(req.getJobLevel()));
        }
        if (req.getHeadcountLimit() != null) {
            existed.setHeadcountLimit(req.getHeadcountLimit());
        }
        if (req.getSortNo() != null) {
            existed.setSortNo(req.getSortNo());
        }
        if (req.getStatus() != null) {
            existed.setStatus(req.getStatus());
        }
        if (req.getPositionSeq() != null) {
            existed.setPositionSeq(req.getPositionSeq());
        }
        if (req.getDeptId() != null) {
            existed.setDeptId(req.getDeptId());
        }
        if (req.getDuty() != null) {
            existed.setDuty(trim(req.getDuty()));
        }
        if (req.getRequirement() != null) {
            existed.setRequirement(trim(req.getRequirement()));
        }
        if (req.getRemark() != null) {
            existed.setRemark(trim(req.getRemark()));
        }
        baseMapper.updateById(existed);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
