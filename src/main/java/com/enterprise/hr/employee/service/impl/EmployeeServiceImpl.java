package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeQuery;
import com.enterprise.hr.employee.dto.EmployeeStatusReq;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.dto.convert.EmployeeConverter;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.EmployeeChange;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.EmployeeChangeMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.employee.service.EmployeeService;
import com.enterprise.hr.employee.service.support.DeptScopeSupport;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 员工业务实现。
 *
 * <p>所有数据访问统一走父类的 {@code baseMapper}（而非 {@code this.save/update/delete}），
 * 保证单测中通过反射注入 mock 即可完全隔离数据库。
 *
 * <p>关联 Mapper（岗位 / 异动）与两个 Support 用字段注入：既保持
 * {@code new EmployeeServiceImpl()} 无参构造可用（既有单测依赖），
 * 又避免与 {@code EmployeeChangeService} 形成构造器循环依赖。
 *
 * @author Enterprise HR Team
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    /** 默认每页条数 */
    private static final long DEFAULT_PAGE_SIZE = 10L;
    /** 默认用工性质：1 正式（employee.employee_type，字典表未覆盖时兜底） */
    private static final int DEFAULT_EMPLOYEE_TYPE = 1;
    /** 入职异动类型（BR-02-01：入职即产生一条异动留痕） */
    private static final int CHANGE_TYPE_ENTRY = 1;
    /** 转正异动类型（BR-02-04） */
    private static final int CHANGE_TYPE_REGULAR = 2;
    /** 调部门异动类型（BR-02-04 其它状态变更归入） */
    private static final int CHANGE_TYPE_TRANSFER = 4;
    /** 离职异动类型（BR-02-04） */
    private static final int CHANGE_TYPE_RESIGN = 7;
    /** 工号自动生成的最大重试次数（并发撞号时重新取号） */
    private static final int EMP_NO_MAX_RETRY = 3;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private EmployeeChangeMapper employeeChangeMapper;

    @Autowired
    private EmployeeInfoSupport employeeInfoSupport;

    @Autowired
    private DeptScopeSupport deptScopeSupport;

    // ------------------------------------------------------------------ 写操作

    /**
     * 新增员工（入职办理）。
     *
     * <p>BR-02-01：工号由系统生成且不可修改；BR-02-02：薪资不得为负。
     * 初始状态「待入职」，并写一条入职异动记录，保证履历从第一天起可追溯。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Employee create(EmployeeCreateReq req) {
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw BizException.badRequest("姓名不能为空");
        }
        if (req.getDeptId() == null) {
            throw BizException.badRequest("所属部门不能为空");
        }
        if (req.getPositionId() == null) {
            throw BizException.badRequest("所属岗位不能为空");
        }
        // BR-02-02：薪资不得为负数
        if (req.getSalary() != null && req.getSalary().signum() < 0) {
            throw new BizException(ErrorCode.NEGATIVE_SALARY);
        }

        Employee entity = EmployeeConverter.toEntity(req);
        entity.setId(null);
        // 新入职一律为「待入职」，转正需走状态变更接口以留下异动记录（BR-02-04）
        entity.setStatus(BizConst.EMPLOYEE_PENDING);
        if (entity.getEmployeeType() == null) {
            entity.setEmployeeType(DEFAULT_EMPLOYEE_TYPE);
        }
        if (entity.getHireDate() == null) {
            entity.setHireDate(LocalDate.now());
        }
        // 职位冗余快照：避免列表频繁联表
        entity.setPosition(resolvePositionName(req.getPositionId(), req.getPosition()));
        entity.setCreatedBy(UserContext.currentUserId());
        entity.setUpdatedBy(UserContext.currentUserId());

        boolean autoEmpNo = !StringUtils.hasText(req.getEmpNo());
        if (autoEmpNo) {
            entity.setEmpNo(nextEmpNo());
        } else {
            String empNo = req.getEmpNo().trim();
            if (existsEmpNo(empNo)) {
                throw new BizException(ErrorCode.EMP_NO_DUPLICATED, "工号已存在: " + empNo);
            }
            entity.setEmpNo(empNo);
        }
        insertWithEmpNoRetry(entity, autoEmpNo);

        recordEntryChange(entity);
        return entity;
    }

    /** 修改员工（局部更新，未传字段保持原值） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Employee update(Long id, EmployeeUpdateReq req) {
        if (id == null) {
            throw BizException.badRequest("员工 id 不能为空");
        }
        if (req == null) {
            throw BizException.badRequest("请求参数不能为空");
        }

        Employee existed = baseMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: id=" + id);
        }

        // BR-02-01：工号是唯一标识且被外部系统引用，不允许修改
        if (StringUtils.hasText(req.getEmpNo())
                && !req.getEmpNo().trim().equals(existed.getEmpNo())) {
            throw BizException.badRequest("工号不可修改");
        }
        // BR-02-02：薪资不得为负数
        if (req.getSalary() != null && req.getSalary().signum() < 0) {
            throw new BizException(ErrorCode.NEGATIVE_SALARY);
        }

        EmployeeConverter.applyUpdate(req, existed);
        // 改岗位时同步刷新职位名快照（快照与岗位主数据不能长期不一致）
        if (req.getPositionId() != null) {
            String fallback = req.getPosition() != null ? req.getPosition() : existed.getPosition();
            existed.setPosition(resolvePositionName(req.getPositionId(), fallback));
        }
        existed.setUpdatedBy(UserContext.currentUserId());
        baseMapper.updateById(existed);
        return existed;
    }

    /**
     * BR-02-03 逻辑删除：仅置离职状态与离职日期。
     *
     * <p>不做物理删除：员工的考勤、薪资、绩效、合同等历史数据都要保留可追溯。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw BizException.badRequest("员工 id 不能为空");
        }
        Employee existed = baseMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在或已删除: id=" + id);
        }
        existed.setStatus(BizConst.EMPLOYEE_RESIGNED);
        existed.setLeaveDate(LocalDate.now());
        existed.setUpdatedBy(UserContext.currentUserId());
        baseMapper.updateById(existed);
    }

    /** 批量删除：与单个删除保持同一 BR-02-03 语义（置离职，不物理删除） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.badRequest("待删除 id 列表不能为空");
        }
        Employee patch = new Employee();
        patch.setStatus(BizConst.EMPLOYEE_RESIGNED);
        patch.setLeaveDate(LocalDate.now());
        patch.setUpdatedBy(UserContext.currentUserId());
        int rows = baseMapper.update(patch, Wrappers.<Employee>lambdaQuery().in(Employee::getId, ids));
        if (rows < 1) {
            throw new IllegalStateException("批量删除失败");
        }
    }

    /**
     * 员工状态变更（离职 / 转正 / 试用 …）。
     *
     * <p>BR-02-04：状态变更需留痕 —— 先写 {@code employee_change} 再改主数据，
     * 两者在同一事务内，不会出现"状态变了但没有异动记录"的审计断点。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, EmployeeStatusReq req) {
        if (req == null || req.getStatus() == null) {
            throw BizException.badRequest("缺少必填字段：status");
        }
        Employee existed = baseMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + id);
        }
        int to = req.getStatus();
        if (!isValidStatus(to)) {
            throw BizException.badRequest("非法的在职状态：" + to);
        }

        LocalDate today = LocalDate.now();
        String reason = req.getReason() == null ? "" : req.getReason();

        EmployeeChange change = new EmployeeChange();
        change.setEmployeeId(existed.getId());
        change.setChangeType(resolveChangeType(to));
        // 状态变更不涉及部门/职位/薪资调整，前后快照一致（保持异动记录结构完整）
        change.setBeforeDeptId(existed.getDeptId());
        change.setAfterDeptId(existed.getDeptId());
        change.setBeforePosition(existed.getPosition());
        change.setAfterPosition(existed.getPosition());
        change.setBeforeSalary(existed.getSalary());
        change.setAfterSalary(existed.getSalary());
        change.setEffectDate(today);
        change.setReason(reason);
        change.setApprovalStatus(BizConst.APPROVAL_APPROVED);
        change.setOperatorId(UserContext.currentEmployeeId());
        employeeChangeMapper.insert(change);

        existed.setStatus(to);
        if (to == BizConst.EMPLOYEE_RESIGNED) {
            existed.setLeaveDate(today);
            existed.setLeaveReason(reason);
        }
        if (to == BizConst.EMPLOYEE_ACTIVE && existed.getRegularDate() == null) {
            existed.setRegularDate(today);
        }
        existed.setUpdatedBy(UserContext.currentUserId());
        baseMapper.updateById(existed);
    }

    // ------------------------------------------------------------------ 查询

    @Override
    public Employee getByEmpNo(String empNo) {
        if (!StringUtils.hasText(empNo)) {
            throw BizException.badRequest("工号不能为空");
        }
        List<Employee> list = baseMapper.selectList(
                Wrappers.<Employee>lambdaQuery().eq(Employee::getEmpNo, empNo.trim()));
        if (list == null || list.isEmpty()) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + empNo);
        }
        return list.get(0);
    }

    /** 旧契约分页（关键字匹配姓名/工号），保留给内部调用与既有测试 */
    @Override
    public IPage<Employee> pageQuery(long current, long size, String keyword) {
        long pageNo = current < 1 ? 1L : current;
        long pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;

        LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 姓名 或 工号 命中即可
            wrapper.and(w -> w.like(Employee::getName, kw).or().like(Employee::getEmpNo, kw));
        }
        wrapper.orderByAsc(Employee::getEmpNo);

        return baseMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    /**
     * 员工分页（新契约 F-02-06）。
     *
     * <p>关键字匹配姓名 / 工号 / 手机号；按工号升序，保证翻页稳定。
     * NF-10 行级数据范围：全量 / 本部门及下级 / 仅本人。
     */
    @Override
    public PageResult<EmployeeVO> pageQuery(EmployeeQuery query) {
        EmployeeQuery q = query == null ? new EmployeeQuery() : query;
        LoginUser user = UserContext.require();

        LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(Employee::getName, kw)
                    .or().like(Employee::getEmpNo, kw)
                    .or().like(Employee::getPhone, kw));
        }
        if (q.getDeptId() != null) {
            wrapper.eq(Employee::getDeptId, q.getDeptId());
        }
        if (q.getPositionId() != null) {
            wrapper.eq(Employee::getPositionId, q.getPositionId());
        }
        if (q.getStatus() != null) {
            wrapper.eq(Employee::getStatus, q.getStatus());
        }
        applyDataScope(wrapper, user);
        wrapper.orderByAsc(Employee::getEmpNo);

        IPage<Employee> page = baseMapper.selectPage(q.toPage(), wrapper);
        List<EmployeeVO> vos = toVOList(page.getRecords());
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 员工详情。
     *
     * <p>NF-10：按 ID 直查必须显式校验行级范围 —— 列表可以靠 WHERE 过滤，
     * 详情接口若不校验，改一下 URL 的 id 就能看到别人的身份证与银行卡。
     */
    @Override
    public EmployeeVO getDetail(Long id) {
        if (id == null) {
            throw BizException.badRequest("缺少必填参数：id");
        }
        Employee entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + id);
        }
        LoginUser user = UserContext.require();
        if (!canAccess(user, entity)) {
            throw BizException.forbidden("无权查看他人档案（NF-10 行级隔离）");
        }
        return toVO(entity);
    }

    /** 某员工的异动记录，按生效日期倒序 */
    @Override
    public List<EmployeeChangeVO> listChanges(Long id) {
        if (id == null) {
            throw BizException.badRequest("缺少必填参数：id");
        }
        List<EmployeeChange> changes = employeeChangeMapper.selectList(
                Wrappers.<EmployeeChange>lambdaQuery()
                        .eq(EmployeeChange::getEmployeeId, id)
                        .orderByDesc(EmployeeChange::getEffectDate)
                        .orderByDesc(EmployeeChange::getId));
        return toChangeVOList(changes);
    }

    // ------------------------------------------------------------------ VO 组装

    @Override
    public EmployeeVO toVO(Employee entity) {
        if (entity == null) {
            return null;
        }
        List<EmployeeVO> vos = toVOList(Collections.singletonList(entity));
        return vos.isEmpty() ? null : vos.get(0);
    }

    /**
     * 实体列表 → VO 列表。
     *
     * <p>部门名与主管姓名用<b>批量 IN 查询</b>一次补齐（整页最多两次 SQL），
     * 不在循环里逐条查库。
     */
    @Override
    public List<EmployeeVO> toVOList(List<Employee> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<EmployeeVO> vos = EmployeeConverter.toVOList(entities);

        List<Long> deptIds = new ArrayList<>(vos.size());
        List<Long> managerIds = new ArrayList<>(vos.size());
        for (EmployeeVO vo : vos) {
            deptIds.add(vo.getDeptId());
            managerIds.add(vo.getManagerId());
        }
        Map<Long, String> deptNames = employeeInfoSupport.loadDeptNames(deptIds);
        Map<Long, Employee> managers = employeeInfoSupport.loadEmployees(managerIds);

        for (EmployeeVO vo : vos) {
            vo.setDeptName(deptNames.get(vo.getDeptId()));
            Employee manager = vo.getManagerId() == null ? null : managers.get(vo.getManagerId());
            vo.setManagerName(manager == null ? null : manager.getName());
        }
        return vos;
    }

    // ------------------------------------------------------------------ 内部工具

    /** NF-10 行级范围 → WHERE 条件 */
    private void applyDataScope(LambdaQueryWrapper<Employee> wrapper, LoginUser user) {
        if (DataScopeHelper.isUnlimited(user)) {
            return;
        }
        if (DataScopeHelper.isSelfOnly(user)) {
            Long employeeId = user.getEmployeeId();
            if (employeeId == null) {
                // 账号未关联员工档案：可见范围为空，而不是"不过滤 = 全量"
                wrapper.apply("1 = 0");
                return;
            }
            wrapper.eq(Employee::getId, employeeId);
            return;
        }
        // 部门主管：本部门及全部下级部门
        List<Long> deptIds = deptScopeSupport.deptAndDescendantIds(user.getDeptId());
        if (deptIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(Employee::getDeptId, deptIds);
    }

    /** 单个员工是否在当前用户可见范围内（NF-10） */
    private boolean canAccess(LoginUser user, Employee target) {
        if (DataScopeHelper.isUnlimited(user)) {
            return true;
        }
        if (DataScopeHelper.isSelfOnly(user)) {
            return target.getId() != null && target.getId().equals(user.getEmployeeId());
        }
        return deptScopeSupport.isInSubtree(user.getDeptId(), target.getDeptId());
    }

    /** {@code status} → 异动类型（BR-02-04）：离职 7 / 转正 2 / 其它 4 */
    private static int resolveChangeType(int status) {
        if (status == BizConst.EMPLOYEE_RESIGNED) {
            return CHANGE_TYPE_RESIGN;
        }
        if (status == BizConst.EMPLOYEE_ACTIVE) {
            return CHANGE_TYPE_REGULAR;
        }
        return CHANGE_TYPE_TRANSFER;
    }

    private static boolean isValidStatus(int status) {
        return status == BizConst.EMPLOYEE_RESIGNED
                || status == BizConst.EMPLOYEE_ACTIVE
                || status == BizConst.EMPLOYEE_PROBATION
                || status == BizConst.EMPLOYEE_PENDING;
    }

    /** 工号是否已存在 */
    private boolean existsEmpNo(String empNo) {
        Long count = baseMapper.selectCount(Wrappers.<Employee>lambdaQuery().eq(Employee::getEmpNo, empNo));
        return count != null && count > 0;
    }

    /**
     * 生成下一个工号：{@code E + (全表最大工号序号 + 1)}，如 E0093。
     *
     * <p>取最大值在 SQL 内完成（{@code MAX(CAST(SUBSTRING(emp_no, 2) AS UNSIGNED))}），
     * 不把全部工号拉回内存比较；非数字工号（如导入的 E-IT-001）CAST 结果为 0，不影响取号。
     *
     * <p><b>必须走 {@link EmployeeMapper#selectMaxEmpNoSeq()} 而不是 {@code selectObjs}：</b>
     * 后者会被 {@code @TableLogic} 追加 {@code deleted = 0}，看不到「已逻辑删除但仍占用
     * {@code uk_emp_no} 唯一键」的行，导致自动工号反复撞键、入职办理 409。详见该方法的注释。
     */
    private String nextEmpNo() {
        return BizNoGenerator.empNo(baseMapper.selectMaxEmpNoSeq() + 1);
    }

    /**
     * 插入员工，并在工号唯一键冲突时重新取号重试（BR-02-01 并发生成兜底）。
     *
     * <p>两个并发的入职请求可能算出同一个序号，此时由 {@code employee.emp_no} 的
     * 唯一键拦下后一条：捕获 {@link DuplicateKeyException} 后重新取最大值 + 1 再插入。
     * MySQL 的重复键错误只回滚当前语句，事务未污染，可以安全重试。
     *
     * <p>仅当工号是服务端自动生成时才重试；调用方显式指定工号时直接抛出冲突，
     * 避免"用户指定 E9999 却静默变成了 E0100"。
     */
    private void insertWithEmpNoRetry(Employee entity, boolean autoEmpNo) {
        int attempt = 0;
        while (true) {
            try {
                baseMapper.insert(entity);
                return;
            } catch (DuplicateKeyException e) {
                if (!autoEmpNo || ++attempt >= EMP_NO_MAX_RETRY) {
                    throw e;
                }
                entity.setId(null);
                entity.setEmpNo(nextEmpNo());
            }
        }
    }

    /** 入职异动留痕：after 取新员工值，reason 固定「入职办理」 */
    private void recordEntryChange(Employee employee) {
        EmployeeChange change = new EmployeeChange();
        change.setEmployeeId(employee.getId());
        change.setChangeType(CHANGE_TYPE_ENTRY);
        change.setBeforeDeptId(null);
        change.setAfterDeptId(employee.getDeptId());
        change.setBeforePosition(null);
        change.setAfterPosition(employee.getPosition());
        change.setBeforeSalary(null);
        change.setAfterSalary(employee.getSalary());
        change.setEffectDate(employee.getHireDate() == null ? LocalDate.now() : employee.getHireDate());
        change.setReason("入职办理");
        change.setApprovalStatus(BizConst.APPROVAL_APPROVED);
        change.setOperatorId(UserContext.currentEmployeeId());
        employeeChangeMapper.insert(change);
    }

    /** 岗位 ID → 岗位名快照（查不到时回退到调用方给的名称） */
    private String resolvePositionName(Long positionId, String fallback) {
        if (positionId == null) {
            return fallback;
        }
        Position position = positionMapper.selectById(positionId);
        return position == null || !StringUtils.hasText(position.getPositionName())
                ? fallback
                : position.getPositionName();
    }

    /** 异动记录列表 → VO 列表（批量补员工姓名与前后部门名） */
    private List<EmployeeChangeVO> toChangeVOList(List<EmployeeChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return Collections.emptyList();
        }
        List<EmployeeChangeVO> vos = new ArrayList<>(changes.size());
        for (EmployeeChange change : changes) {
            vos.add(EmployeeChangeVO.from(change));
        }
        employeeInfoSupport.fillChangeFields(vos);
        return vos;
    }
}
