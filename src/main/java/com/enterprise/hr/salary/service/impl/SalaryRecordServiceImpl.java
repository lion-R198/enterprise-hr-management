package com.enterprise.hr.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.attendance.entity.AttendanceMonth;
import com.enterprise.hr.attendance.mapper.AttendanceMonthMapper;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.salary.dto.PayslipDetailVO;
import com.enterprise.hr.salary.dto.SalaryCalculateReq;
import com.enterprise.hr.salary.dto.SalaryItemVO;
import com.enterprise.hr.salary.dto.SalaryPayReq;
import com.enterprise.hr.salary.dto.SalaryRecordQuery;
import com.enterprise.hr.salary.dto.SalaryRecordVO;
import com.enterprise.hr.salary.entity.SalaryItem;
import com.enterprise.hr.salary.entity.SalaryRecord;
import com.enterprise.hr.salary.mapper.SalaryItemMapper;
import com.enterprise.hr.salary.mapper.SalaryRecordMapper;
import com.enterprise.hr.salary.service.SalaryRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 月度薪资记录业务实现：核算 / 复核 / 发放 / 薪资条。
 *
 * <p>核算算法逐式对齐前端 mock（{@code src/mock/index.js} 第 657~676 行），
 * 明细项构造对齐 {@code src/mock/db.js} 的 {@code buildSalaryItems(record)}（11 条），
 * 保证 mock 与真实后端产出可逐字段比对。
 *
 * <p>金额一律 {@link BigDecimal} + {@code setScale(2, HALF_UP)}，禁止 double 参与运算
 * ——浮点误差会在"实发合计"上暴露为分位偏差，属于薪资场景不可接受的错误。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class SalaryRecordServiceImpl implements SalaryRecordService {

    // ---------------- 核算算法参数（对齐 mock，改一处即改口径） ----------------
    /** 基本工资占比 70% */
    private static final BigDecimal RATIO_BASE = new BigDecimal("0.7");
    /** 岗位工资占比 20% */
    private static final BigDecimal RATIO_POST = new BigDecimal("0.2");
    /** 绩效工资占比 10% */
    private static final BigDecimal RATIO_PERFORMANCE = new BigDecimal("0.1");
    /** 固定补贴 500 元 */
    private static final BigDecimal FIXED_ALLOWANCE = new BigDecimal("500");
    /** 社保个人比例 10.5%（养老 8% + 医疗 2% + 失业 0.5%） */
    private static final BigDecimal RATIO_SOCIAL_PERSONAL = new BigDecimal("0.105");
    /** 公积金个人比例 12% */
    private static final BigDecimal RATIO_FUND_PERSONAL = new BigDecimal("0.12");
    /** 社保企业比例 26.5% */
    private static final BigDecimal RATIO_SOCIAL_COMPANY = new BigDecimal("0.265");
    /** 个税起征点 5000 元 */
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");
    /** 简化个税税率 10% */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.1");

    // ---------------- salary_item.item_type ----------------
    /** 1 应发项 */
    private static final int ITEM_TYPE_PAYABLE = 1;
    /** 2 扣款项 */
    private static final int ITEM_TYPE_DEDUCT = 2;
    /** 3 企业承担项 */
    private static final int ITEM_TYPE_COMPANY = 3;

    /** 2 位小数、四舍五入的 0 值 */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    /**
     * 历史核算人标记的兼容解析：{@code remark} 中的 {@code CALC_BY=<userId>}。
     *
     * <p><b>仅用于读取 2026-09-15 之前写入的历史数据。</b>那时表内没有核算人列，
     * BR-05-03（复核人不得与核算人相同）只能靠这个结构化标记成立。
     * 现已补入 {@code salary_record.calc_by} 列，新数据一律写该列；
     * 保留本解析器是为了让升级前已生成的薪资记录在复核时仍能正确拦截。
     */
    private static final Pattern CALC_BY_PATTERN = Pattern.compile("CALC_BY=(\\d+)");

    /** 发放批次号日期部分格式：yyyyMMdd */
    private static final DateTimeFormatter BATCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryItemMapper salaryItemMapper;
    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;
    private final AttendanceMonthMapper attendanceMonthMapper;

    // ------------------------------------------------------------------ F-05-02

    @Override
    public PageResult<SalaryRecordVO> page(SalaryRecordQuery query) {
        SalaryRecordQuery condition = query == null ? new SalaryRecordQuery() : query;
        LoginUser user = UserContext.require();

        LambdaQueryWrapper<SalaryRecord> wrapper = Wrappers.<SalaryRecord>lambdaQuery();
        // NF-10 行级数据范围：主管仅本部门、普通员工仅本人，HR/财务/管理员/决策层不受限
        if (!DataScopeHelper.isUnlimited(user)) {
            if (DataScopeHelper.isSelfOnly(user)) {
                wrapper.eq(SalaryRecord::getEmployeeId, user.getEmployeeId());
            } else {
                wrapper.eq(SalaryRecord::getDeptId, user.getDeptId());
            }
        }
        if (condition.getYear() != null) {
            wrapper.eq(SalaryRecord::getSalaryYear, condition.getYear());
        }
        if (condition.getMonth() != null) {
            wrapper.eq(SalaryRecord::getSalaryMonth, condition.getMonth());
        }
        if (condition.getPayStatus() != null) {
            wrapper.eq(SalaryRecord::getPayStatus, condition.getPayStatus());
        }
        if (condition.getDeptId() != null) {
            wrapper.eq(SalaryRecord::getDeptId, condition.getDeptId());
        }
        if (StringUtils.hasText(condition.getKeyword())) {
            List<Long> employeeIds = matchEmployeeIds(condition.getKeyword().trim());
            if (employeeIds.isEmpty()) {
                // 关键字未命中任何员工：直接返回空页，避免 IN () 语法错误
                return PageResult.empty(condition.safeCurrent(), condition.safeSize());
            }
            wrapper.in(SalaryRecord::getEmployeeId, employeeIds);
        }
        // 排序：年降序 → 月降序 → 员工ID升序（与 mock 第 645 行一致）
        wrapper.orderByDesc(SalaryRecord::getSalaryYear)
                .orderByDesc(SalaryRecord::getSalaryMonth)
                .orderByAsc(SalaryRecord::getEmployeeId);

        IPage<SalaryRecord> result = salaryRecordMapper.selectPage(condition.toPage(), wrapper);
        return new PageResult<>(toVOList(result.getRecords()), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    // ------------------------------------------------------------------ 核算

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculate(SalaryCalculateReq req) {
        LoginUser user = UserContext.require();
        Integer year = req.getYear();
        Integer month = req.getMonth();

        // BR-05-02：核算必须以"已封账"的考勤月报为输入
        Long unlocked = attendanceMonthMapper.selectCount(Wrappers.<AttendanceMonth>lambdaQuery()
                .eq(AttendanceMonth::getRecordYear, year)
                .eq(AttendanceMonth::getRecordMonth, month)
                .eq(AttendanceMonth::getLockStatus, BizConst.MONTH_UNLOCKED));
        if (unlocked != null && unlocked > 0) {
            throw new BizException(ErrorCode.SALARY_MONTH_UNLOCKED,
                    "有 " + unlocked + " 名员工的考勤月报未封账，禁止启动核算（BR-05-02）");
        }

        // 核算范围：非离职员工（status != 0，含在职/试用/待入职）
        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .ne(Employee::getStatus, BizConst.EMPLOYEE_RESIGNED)
                .orderByAsc(Employee::getId));
        if (employees.isEmpty()) {
            return 0;
        }

        // 已在当月经录的员工直接跳过（幂等：重复点击"发起核算"不会产生重复记录）
        Set<Long> existingEmployeeIds = salaryRecordMapper.selectList(Wrappers.<SalaryRecord>lambdaQuery()
                        .eq(SalaryRecord::getSalaryYear, year)
                        .eq(SalaryRecord::getSalaryMonth, month))
                .stream()
                .map(SalaryRecord::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Long calcByUserId = user.getUserId();
        int created = 0;
        for (Employee employee : employees) {
            if (existingEmployeeIds.contains(employee.getId())) {
                continue;
            }
            SalaryRecord record = buildRecord(employee, year, month, calcByUserId);
            salaryRecordMapper.insert(record);
            // 明细项与记录同事务落库：11 条，缺一条即为脏数据
            for (SalaryItem item : buildItems(record)) {
                salaryItemMapper.insert(item);
            }
            created++;
        }
        return created;
    }

    /**
     * 构造薪资记录，逐式照搬 mock 第 660~674 行算法。
     *
     * <pre>
     * 基本 = 月薪 × 70%        岗位 = 月薪 × 20%      绩效 = 月薪 × 10%
     * 补贴 = 500               应发 = 基本 + 岗位 + 绩效 + 补贴
     * 社保个人 = 月薪 × 10.5%   公积金个人 = 月薪 × 12%
     * 个税 = max(0, (应发 - 5000 - 社保个人 - 公积金个人) × 10%)
     * 实发 = 应发 - 社保个人 - 公积金个人 - 个税
     * 社保企业 = 月薪 × 26.5%   公积金企业 = 公积金个人
     * </pre>
     */
    private SalaryRecord buildRecord(Employee employee, Integer year, Integer month, Long calcByUserId) {
        BigDecimal salary = employee.getSalary() == null ? BigDecimal.ZERO : employee.getSalary();

        BigDecimal baseSalary = money(salary.multiply(RATIO_BASE));
        BigDecimal postSalary = money(salary.multiply(RATIO_POST));
        BigDecimal performanceSalary = money(salary.multiply(RATIO_PERFORMANCE));
        BigDecimal allowance = money(FIXED_ALLOWANCE);
        BigDecimal grossPay = money(baseSalary.add(postSalary).add(performanceSalary).add(allowance));

        BigDecimal socialPersonal = money(salary.multiply(RATIO_SOCIAL_PERSONAL));
        BigDecimal fundPersonal = money(salary.multiply(RATIO_FUND_PERSONAL));
        BigDecimal taxable = grossPay.subtract(TAX_THRESHOLD).subtract(socialPersonal).subtract(fundPersonal);
        BigDecimal taxAmount = taxable.signum() <= 0 ? ZERO_AMOUNT : money(taxable.multiply(TAX_RATE));

        BigDecimal socialCompany = money(salary.multiply(RATIO_SOCIAL_COMPANY));
        BigDecimal fundCompany = fundPersonal;
        BigDecimal netPay = money(grossPay.subtract(socialPersonal).subtract(fundPersonal).subtract(taxAmount));

        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employee.getId());
        record.setDeptId(employee.getDeptId());
        record.setSalaryYear(year);
        record.setSalaryMonth(month);
        record.setBaseSalary(baseSalary);
        record.setPostSalary(postSalary);
        record.setPerformanceSalary(performanceSalary);
        record.setAllowance(allowance);
        record.setOvertimePay(ZERO_AMOUNT);
        record.setOtherAdd(ZERO_AMOUNT);
        record.setAttendanceDeduct(ZERO_AMOUNT);
        record.setOtherDeduct(ZERO_AMOUNT);
        record.setGrossPay(grossPay);
        record.setSocialPersonal(socialPersonal);
        record.setSocialCompany(socialCompany);
        record.setFundPersonal(fundPersonal);
        record.setFundCompany(fundCompany);
        record.setTaxAmount(taxAmount);
        record.setNetPay(netPay);
        // 核算完成即进入"已核算"，待复核（BR-05-03 三级流转：核算 → 复核 → 发放）
        record.setPayStatus(BizConst.PAY_CALCULATED);
        // 未发放故未锁定（BR-05-04 的锁定只发生在发放之后）
        record.setLockStatus(BizConst.MONTH_UNLOCKED);
        // BR-05-03：核算人落独立列，remark 归还给业务备注使用
        record.setCalcBy(calcByUserId);
        record.setRemark("");
        return record;
    }

    /**
     * 构造 11 条薪资明细项，逐条对齐 {@code db.js#buildSalaryItems(record)}：
     * 1 应发项 5 条（BASE/POST/PERF/ALLOW/OT）、2 扣款项 4 条（ATT/SOCIAL/FUND/TAX）、
     * 3 企业承担项 2 条（SOCIAL_C/FUND_C）。
     */
    private List<SalaryItem> buildItems(SalaryRecord record) {
        Long recordId = record.getId();
        List<SalaryItem> items = new ArrayList<>(11);
        // ---- 1 应发项 ----
        items.add(item(recordId, ITEM_TYPE_PAYABLE, "BASE", "基本工资", record.getBaseSalary(), 1, null));
        items.add(item(recordId, ITEM_TYPE_PAYABLE, "POST", "岗位工资", record.getPostSalary(), 2, null));
        items.add(item(recordId, ITEM_TYPE_PAYABLE, "PERF", "绩效工资", record.getPerformanceSalary(), 3, null));
        items.add(item(recordId, ITEM_TYPE_PAYABLE, "ALLOW", "补贴合计", record.getAllowance(), 4, null));
        items.add(item(recordId, ITEM_TYPE_PAYABLE, "OT", "加班费", record.getOvertimePay(), 5, null));
        // ---- 2 扣款项 ----
        items.add(item(recordId, ITEM_TYPE_DEDUCT, "ATT", "考勤扣款", record.getAttendanceDeduct(), 10,
                "迟到/早退/旷工按参数折算"));
        items.add(item(recordId, ITEM_TYPE_DEDUCT, "SOCIAL", "社保个人", record.getSocialPersonal(), 11,
                "养老8%+医疗2%+失业0.5%"));
        items.add(item(recordId, ITEM_TYPE_DEDUCT, "FUND", "公积金个人", record.getFundPersonal(), 12, null));
        items.add(item(recordId, ITEM_TYPE_DEDUCT, "TAX", "个人所得税", record.getTaxAmount(), 13, "累计预扣法"));
        // ---- 3 企业承担项 ----
        items.add(item(recordId, ITEM_TYPE_COMPANY, "SOCIAL_C", "社保企业", record.getSocialCompany(), 20, null));
        items.add(item(recordId, ITEM_TYPE_COMPANY, "FUND_C", "公积金企业", record.getFundCompany(), 21, null));
        return items;
    }

    private SalaryItem item(Long recordId, int itemType, String code, String name,
                            BigDecimal amount, int sortNo, String remark) {
        SalaryItem item = new SalaryItem();
        item.setSalaryRecordId(recordId);
        item.setItemType(itemType);
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemAmount(money(amount));
        item.setSortNo(sortNo);
        item.setRemark(remark == null ? "" : remark);
        return item;
    }

    // ------------------------------------------------------------------ 复核 / 发放

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id) {
        LoginUser user = UserContext.require();
        SalaryRecord record = requireRecord(id);

        // BR-05-04：发放后记录锁定，任何后续修改（含复核）一律拒绝
        if (isLocked(record)) {
            throw new BizException(ErrorCode.SALARY_LOCKED);
        }
        // BR-05-03：状态机 1(已核算) → 2(已复核)
        Integer payStatus = record.getPayStatus();
        if (payStatus == null || payStatus != BizConst.PAY_CALCULATED) {
            throw new BizException(ErrorCode.SALARY_STATUS_ILLEGAL, "仅“已核算”状态的薪资记录可复核（BR-05-03）");
        }
        // BR-05-03：复核人不得与核算人相同；核算人取自 remark 中的 CALC_BY 标记
        // 优先读独立列；历史数据（calc_by 列补入之前生成）回退到 remark 中的 CALC_BY 标记
        Long calcBy = record.getCalcBy() != null
                ? record.getCalcBy()
                : parseCalcBy(record.getRemark());
        if (calcBy != null && calcBy.equals(user.getUserId())) {
            throw new BizException(ErrorCode.SALARY_REVIEWER_CONFLICT);
        }

        SalaryRecord update = new SalaryRecord();
        update.setId(id);
        update.setPayStatus(BizConst.PAY_REVIEWED);
        update.setAuditBy(user.getUserId());
        salaryRecordMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, SalaryPayReq req) {
        UserContext.require();
        SalaryRecord record = requireRecord(id);

        // BR-05-04：已发放并锁定的记录禁止重复发放
        if (isLocked(record)) {
            throw new BizException(ErrorCode.SALARY_LOCKED);
        }
        // BR-05-03：状态机 2(已复核) → 3(已发放)
        Integer payStatus = record.getPayStatus();
        if (payStatus == null || payStatus != BizConst.PAY_REVIEWED) {
            throw new BizException(ErrorCode.SALARY_STATUS_ILLEGAL, "仅“已复核”状态的薪资记录可发放（BR-05-03）");
        }

        SalaryRecord update = new SalaryRecord();
        update.setId(id);
        update.setPayStatus(BizConst.PAY_PAID);
        update.setPayDate(LocalDate.now());
        update.setPayBatchNo(resolveBatchNo(req));
        // BR-05-04：发放即锁定，后续任何修改都要拒绝
        update.setLockStatus(BizConst.MONTH_LOCKED);
        salaryRecordMapper.updateById(update);
    }

    /** 批次号：优先取入参，缺省按 {@code PAY + yyyyMMdd + 001} 生成（契约格式，非 BizNoGenerator 的 前缀+年+5位） */
    private String resolveBatchNo(SalaryPayReq req) {
        if (req != null && StringUtils.hasText(req.getBatchNo())) {
            return req.getBatchNo().trim();
        }
        return "PAY" + LocalDate.now().format(BATCH_DATE_FORMATTER) + "001";
    }

    /** BR-05-04 判定：{@code lock_status = 1} 即已锁定（复用 BizConst.MONTH_LOCKED 语义） */
    private boolean isLocked(SalaryRecord record) {
        Integer lockStatus = record.getLockStatus();
        return lockStatus != null && lockStatus == BizConst.MONTH_LOCKED;
    }

    // ------------------------------------------------------------------ 薪资条

    @Override
    public PageResult<SalaryRecordVO> myPayslips() {
        LoginUser user = UserContext.require();
        if (user.getEmployeeId() == null) {
            // 非员工账号（如纯运维账号）没有薪资条，返回空列表而非报错
            return PageResult.list(List.of());
        }
        List<SalaryRecord> records = salaryRecordMapper.selectList(Wrappers.<SalaryRecord>lambdaQuery()
                .eq(SalaryRecord::getEmployeeId, user.getEmployeeId())
                .orderByDesc(SalaryRecord::getSalaryYear)
                .orderByDesc(SalaryRecord::getSalaryMonth));
        return PageResult.list(toVOList(records));
    }

    @Override
    public PayslipDetailVO payslipItems(Long recordId) {
        LoginUser user = UserContext.require();
        SalaryRecord record = requireRecord(recordId);

        // NF-10 行级隔离：仅本人，或具备薪资查看权的 HR / FINANCE / ADMIN
        boolean self = record.getEmployeeId() != null && record.getEmployeeId().equals(user.getEmployeeId());
        boolean authorized = user.hasAnyRole(RoleCode.HR, RoleCode.FINANCE, RoleCode.ADMIN);
        if (!self && !authorized) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看他人薪资明细（NF-10 行级隔离）");
        }

        List<SalaryItem> items = salaryItemMapper.selectList(Wrappers.<SalaryItem>lambdaQuery()
                .eq(SalaryItem::getSalaryRecordId, recordId)
                .orderByAsc(SalaryItem::getSortNo));

        Employee employee = record.getEmployeeId() == null ? null : employeeMapper.selectById(record.getEmployeeId());
        Dept dept = record.getDeptId() == null ? null : deptMapper.selectById(record.getDeptId());

        return PayslipDetailVO.builder()
                .record(toVO(record, employee, dept))
                .items(items.stream().map(this::toItemVO).collect(Collectors.toList()))
                .build();
    }

    // ------------------------------------------------------------------ 内部工具

    private SalaryRecord requireRecord(Long id) {
        if (id == null) {
            throw BizException.badRequest("薪资记录 id 不能为空");
        }
        SalaryRecord record = salaryRecordMapper.selectById(id);
        if (record == null) {
            throw BizException.notFound("薪资记录不存在");
        }
        return record;
    }

    /** 关键字（姓名/工号）命中的员工 ID，未命中返回空列表 */
    private List<Long> matchEmployeeIds(String keyword) {
        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .and(w -> w.like(Employee::getName, keyword).or().like(Employee::getEmpNo, keyword)));
        return employees.stream()
                .map(Employee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 批量补全 employee_name / emp_no / dept_name，避免逐行查库 */
    private List<SalaryRecordVO> toVOList(List<SalaryRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> employeeIds = records.stream()
                .map(SalaryRecord::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            employeeMap = employeeMapper.selectBatchIds(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        }
        Set<Long> deptIds = records.stream()
                .map(SalaryRecord::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Dept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(Dept::getId, d -> d, (a, b) -> a));
        }

        List<SalaryRecordVO> list = new ArrayList<>(records.size());
        for (SalaryRecord record : records) {
            Employee employee = employeeMap.get(record.getEmployeeId());
            Dept dept = deptMap.get(record.getDeptId());
            list.add(toVO(record, employee, dept));
        }
        return list;
    }

    private SalaryRecordVO toVO(SalaryRecord record, Employee employee, Dept dept) {
        return SalaryRecordVO.builder()
                .id(record.getId())
                .employeeId(record.getEmployeeId())
                .employeeName(employee == null ? null : employee.getName())
                .empNo(employee == null ? null : employee.getEmpNo())
                .deptId(record.getDeptId())
                .deptName(dept == null ? null : dept.getName())
                .salaryYear(record.getSalaryYear())
                .salaryMonth(record.getSalaryMonth())
                .baseSalary(record.getBaseSalary())
                .postSalary(record.getPostSalary())
                .performanceSalary(record.getPerformanceSalary())
                .allowance(record.getAllowance())
                .overtimePay(record.getOvertimePay())
                .otherAdd(record.getOtherAdd())
                .attendanceDeduct(record.getAttendanceDeduct())
                .otherDeduct(record.getOtherDeduct())
                .grossPay(record.getGrossPay())
                .socialPersonal(record.getSocialPersonal())
                .socialCompany(record.getSocialCompany())
                .fundPersonal(record.getFundPersonal())
                .fundCompany(record.getFundCompany())
                .taxAmount(record.getTaxAmount())
                .netPay(record.getNetPay())
                .payStatus(record.getPayStatus())
                .payDate(record.getPayDate())
                .payBatchNo(record.getPayBatchNo())
                .lockStatus(record.getLockStatus())
                .calcBy(record.getCalcBy())
                .auditBy(record.getAuditBy())
                .build();
    }

    private SalaryItemVO toItemVO(SalaryItem item) {
        return SalaryItemVO.builder()
                .id(item.getId())
                .salaryRecordId(item.getSalaryRecordId())
                .itemType(item.getItemType())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .itemAmount(item.getItemAmount())
                .sortNo(item.getSortNo())
                .remark(item.getRemark())
                .build();
    }

    /** 金额规整：2 位小数、四舍五入（mock 的 money() 等价实现） */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 从历史 {@code remark} 标记中解析核算人用户 ID（兼容读取，见 {@link #CALC_BY_PATTERN}）。
     *
     * @return 解析到的用户 ID；无标记（人工导入的历史数据）返回 {@code null}，表示无法判定
     */
    private Long parseCalcBy(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        Matcher matcher = CALC_BY_PATTERN.matcher(remark);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
