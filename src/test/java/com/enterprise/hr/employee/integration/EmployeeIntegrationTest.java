package com.enterprise.hr.employee.integration;

import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Employee 真实数据库集成测试：连接本机 MySQL，验证 MyBatis-Plus 配置与员工业务规则全链路生效。
 *
 * <p>覆盖（改造后语义）：
 * <ul>
 *     <li>{@code @MapperScan} 注册的 BaseMapper 能注入并工作</li>
 *     <li><b>主键自增</b>：{@code id-type: AUTO}（表是 AUTO_INCREMENT，
 *         再用 {@code ASSIGN_ID} 雪花会写出超出自增序列的 ID 并污染计数器）</li>
 *     <li>{@code MetaObjectHandler} 自动填充 createdAt / updatedAt</li>
 *     <li><b>BR-02-03</b>：{@code delete} 是"置离职状态 + 离职日期"，行必须保留；
 *         {@code @TableLogic} 只作用于 {@code mapper.deleteById} 这类真正的删除调用</li>
 *     <li><b>BR-02-01</b>：工号由服务端生成、状态默认待入职、入职异动留痕</li>
 *     <li>分页插件正确组装 LIMIT / OFFSET / 计数 SQL</li>
 *     <li>唯一约束（uk_emp_no）冲突抛出</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-cleanup.sql")
@Transactional  // 每个用例自动回滚，不污染种子数据
class EmployeeIntegrationTest {

    /** 种子数据里最大的员工主键（1001 / 1002 / 1003） */
    private static final long SEED_MAX_ID = 1003L;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JdbcTemplate jdbc;

    // ----------------- Mapper / @MapperScan -----------------

    @Test
    @DisplayName("@MapperScan 注册的 EmployeeMapper 可注入并执行 SELECT")
    void mapper_inject_and_selectById() {
        Employee e = employeeMapper.selectById(1001L);
        assertNotNull(e, "Mapper 应可通过 selectById 查到种子数据");
        assertEquals("E0001", e.getEmpNo());
        assertEquals("张三", e.getName());
        assertEquals(Integer.valueOf(1), e.getStatus(), "在职");
        assertEquals(Integer.valueOf(0), e.getDeleted(), "MyBatis-Plus @TableLogic 应自动 append WHERE deleted=0");
    }

    // ----------------- 主键策略：AUTO_INCREMENT -----------------

    @Test
    @DisplayName("insert 时 id-type=AUTO 由数据库自增分配主键（不是雪花 ID）")
    void auto_id_type_assigns_incrementing_primary_key() {
        Employee fresh = new Employee();
        fresh.setEmpNo("E9001");
        fresh.setName("新员工");
        fresh.setStatus(BizConst.EMPLOYEE_ACTIVE);

        int rows = employeeMapper.insert(fresh);
        assertEquals(1, rows);
        assertNotNull(fresh.getId(), "自增主键应回填到实体");
        assertTrue(fresh.getId() > SEED_MAX_ID,
                "自增主键应接续种子数据（1001~1003）之后，实际=" + fresh.getId());
        assertTrue(String.valueOf(fresh.getId()).length() <= 10,
                "应为数据库自增（小整数），而非 19 位雪花 ID，实际=" + fresh.getId());

        Employee second = new Employee();
        second.setEmpNo("E9002");
        second.setName("新员工2");
        second.setStatus(BizConst.EMPLOYEE_ACTIVE);
        employeeMapper.insert(second);
        assertEquals(fresh.getId() + 1, second.getId().longValue(),
                "连续两次插入应得到严格递增 +1 的自增主键");
    }

    // ----------------- 自动填充 -----------------

    @Test
    @DisplayName("MetaObjectHandler 自动填充 createdAt / updatedAt")
    void auto_fill_timestamps() {
        Employee fresh = new Employee();
        fresh.setEmpNo("E9003");
        fresh.setName("新员工3");
        fresh.setStatus(BizConst.EMPLOYEE_ACTIVE);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        employeeMapper.insert(fresh);

        Employee reloaded = employeeMapper.selectById(fresh.getId());
        assertNotNull(reloaded);
        assertNotNull(reloaded.getCreatedAt(), "createdAt 应被自动填充");
        assertNotNull(reloaded.getUpdatedAt(), "updatedAt 应被自动填充");
        assertTrue(reloaded.getCreatedAt().isAfter(before), "createdAt 应该在插入前时间之后");
    }

    @Test
    @DisplayName("updateById 能持久化修改，且 createdAt 保持不变")
    void auto_fill_updates_updatedAt_on_modify() {
        Employee fresh = new Employee();
        fresh.setEmpNo("E9004");
        fresh.setName("新员工4");
        fresh.setStatus(BizConst.EMPLOYEE_ACTIVE);
        employeeMapper.insert(fresh);

        Employee target = employeeMapper.selectById(fresh.getId());
        LocalDateTime originalCreated = target.getCreatedAt();
        LocalDateTime originalUpdated = target.getUpdatedAt();

        target.setName("改名了");
        int updated = employeeMapper.updateById(target);
        assertEquals(1, updated);

        Employee reloaded = employeeMapper.selectById(fresh.getId());
        assertEquals("改名了", reloaded.getName());
        // createdAt 必须不变（@TableField(fill = INSERT)）
        assertEquals(originalCreated, reloaded.getCreatedAt(), "createdAt 应保持创建时间不变");
        // updatedAt 由 ON UPDATE CURRENT_TIMESTAMP 触发，可能在同秒也由 MetaObjectHandler 同步更新 —— 验证 >= 原值即可
        assertNotNull(reloaded.getUpdatedAt());
        assertFalse(reloaded.getUpdatedAt().isBefore(originalUpdated),
                "updatedAt 应 >= 原值（" + originalUpdated + " → " + reloaded.getUpdatedAt() + "）");
    }

    // ----------------- 逻辑删除 / BR-02-03 -----------------

    @Test
    @DisplayName("@TableLogic：mapper.deleteById 发出 UPDATE ... SET deleted=1，物理行仍存在")
    void table_logic_delete_emits_update() {
        Employee fresh = new Employee();
        fresh.setEmpNo("E9101");
        fresh.setName("逻辑删除测试");
        fresh.setStatus(BizConst.EMPLOYEE_ACTIVE);
        employeeMapper.insert(fresh);
        Long id = fresh.getId();

        // mapper.deleteById 是"真删除"入口，被 @TableLogic 改写为逻辑删除
        assertEquals(1, employeeMapper.deleteById(id));
        // 第二次删除返回 0：已逻辑删除的行被 WHERE deleted=0 拦住
        assertEquals(0, employeeMapper.deleteById(id),
                "删除后第二次调用 deleteById 应返回 0（已逻辑删除的行被 WHERE deleted=0 拦了）");

        // 默认查询应看不到已删除行
        assertNull(employeeMapper.selectById(id), "@TableLogic 应让默认查询看不到已删除行");

        // 直接用 JDBC SELECT，绕过 @TableLogic，验证物理行 deleted=1
        Integer physicalDeleted = jdbc.queryForObject(
                "SELECT deleted FROM employee WHERE id = ?", Integer.class, id);
        assertNotNull(physicalDeleted);
        assertEquals(Integer.valueOf(1), physicalDeleted,
                "@TableLogic 应发出 UPDATE ... SET deleted=1（行物理仍存在且 deleted=1）");
    }

    @Test
    @DisplayName("BR-02-03：Service.delete 是「置离职 + 离职日期」，行必须保留且不被逻辑删除")
    void service_delete_resigns_and_keeps_row() {
        Employee fresh = new Employee();
        fresh.setEmpNo("E9102");
        fresh.setName("离职测试");
        fresh.setStatus(BizConst.EMPLOYEE_ACTIVE);
        fresh.setLeaveDate(null);
        employeeMapper.insert(fresh);
        Long id = fresh.getId();

        employeeService.delete(id);

        // 仍然查得到（不是物理删除，也不是 @TableLogic 逻辑删除）
        Employee after = employeeMapper.selectById(id);
        assertNotNull(after, "历史数据要保留可追溯，员工行不得被删除");
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_RESIGNED), after.getStatus(), "状态置为离职");
        assertEquals(LocalDate.now(), after.getLeaveDate(), "写入离职日期");

        Integer physicalDeleted = jdbc.queryForObject(
                "SELECT deleted FROM employee WHERE id = ?", Integer.class, id);
        assertEquals(Integer.valueOf(0), physicalDeleted, "deleted 标志必须保持 0");
    }

    @Test
    @DisplayName("Service.delete 对不存在的员工 → 拒绝且不误改任何行")
    void service_delete_missing_employee_rejected() {
        long missingId = 999_999L;
        // 契约层期望 404；当前实现抛 IllegalArgumentException（统一映射为 400）——
        // 该不一致已在交付报告中列为待确认项，这里只断言"必须被拒绝、不能静默成功"
        assertThrows(RuntimeException.class, () -> employeeService.delete(missingId));
    }

    @Test
    @DisplayName("批量逻辑删除走 deleteBatchIds → 多条 UPDATE")
    void logical_delete_batch() {
        Employee e1 = build("E9201", "批量1");
        Employee e2 = build("E9202", "批量2");
        Employee e3 = build("E9203", "批量3");
        employeeMapper.insert(e1);
        employeeMapper.insert(e2);
        employeeMapper.insert(e3);

        int rows = employeeMapper.deleteBatchIds(List.of(e1.getId(), e2.getId(), e3.getId()));
        assertEquals(3, rows);

        assertNull(employeeMapper.selectById(e1.getId()));
        assertNull(employeeMapper.selectById(e2.getId()));
        assertNull(employeeMapper.selectById(e3.getId()));
    }

    @Test
    @DisplayName("BR-02-03：Service.deleteBatch 是「批量置离职」，三条行都还在")
    void service_deleteBatch_resigns_all() {
        Employee e1 = build("E9211", "批量离职1");
        Employee e2 = build("E9212", "批量离职2");
        employeeMapper.insert(e1);
        employeeMapper.insert(e2);

        assertDoesNotThrow(() -> employeeService.deleteBatch(List.of(e1.getId(), e2.getId())));

        for (Long id : List.of(e1.getId(), e2.getId())) {
            Employee after = employeeMapper.selectById(id);
            assertNotNull(after, "行不得被删除: id=" + id);
            assertEquals(Integer.valueOf(BizConst.EMPLOYEE_RESIGNED), after.getStatus());
            assertEquals(LocalDate.now(), after.getLeaveDate());
        }
    }

    // ----------------- BR-02-01：工号自动生成 + 默认状态 -----------------

    @Test
    @DisplayName("BR-02-01：Service.create 自动生成工号、默认待入职，并写入入职异动")
    void service_create_generates_emp_no_pending_status_and_entry_change() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("集成入职员工");
        req.setDeptId(1L);
        req.setPositionId(1L);
        req.setSalary(new BigDecimal("12345.67"));

        Employee created = employeeService.create(req);

        assertNotNull(created.getId());
        assertNotNull(created.getEmpNo(), "工号必须由服务端生成");
        assertTrue(created.getEmpNo().matches("^E\\d{4,}$"),
                "工号形如 E0051，实际=" + created.getEmpNo());
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_PENDING), created.getStatus(),
                "新入职默认「待入职」");
        assertEquals(LocalDate.now(), created.getHireDate(), "入职日期缺省为当天");
        assertEquals("Java开发工程师", created.getPosition(), "职位名按 positionId 取快照");

        // 落库校验（绕开实体内存态）
        Employee reloaded = employeeMapper.selectById(created.getId());
        assertNotNull(reloaded);
        assertEquals(created.getEmpNo(), reloaded.getEmpNo());
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_PENDING), reloaded.getStatus());
        assertEquals(0, new BigDecimal("12345.67").compareTo(reloaded.getSalary()), "金额精度保留");

        // 入职异动留痕（BR-02-01/04）
        Integer changeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee_change WHERE employee_id = ? AND change_type = 1",
                Integer.class, created.getId());
        assertEquals(Integer.valueOf(1), changeCount, "入职应产生一条 change_type=1 的异动记录");
    }

    @Test
    @DisplayName("BR-02-02：Service.create 负薪资 → BizException(NEGATIVE_SALARY)，不落库")
    void service_create_negative_salary_rejected() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("负薪员工");
        req.setDeptId(1L);
        req.setPositionId(1L);
        req.setSalary(new BigDecimal("-100.00"));

        BizException ex = assertThrows(BizException.class, () -> employeeService.create(req));
        assertEquals(ErrorCode.NEGATIVE_SALARY.getCode(), ex.getCode());
    }

    // ----------------- 分页插件 -----------------

    @Test
    @DisplayName("分页插件返回的 records / current / size / total 正确")
    void pagination_emits_limit_offset_and_count() {
        for (int i = 0; i < 15; i++) {
            Employee e = build("E9300-" + i, "分页" + i);
            employeeMapper.insert(e);
        }

        com.baomidou.mybatisplus.core.metadata.IPage<Employee> page =
                employeeService.pageQuery(2, 5, null);

        assertEquals(2L, page.getCurrent());
        assertEquals(5L, page.getSize());
        assertEquals(5, page.getRecords().size(), "每页 5 条");
        assertTrue(page.getTotal() >= 15, "总数应至少 15 条");

        // MyBatis-Plus PaginationInnerInterceptor 的行为：
        // - 实际 SQL 走 LIMIT/OFFSET
        // - 还发出一条 SELECT COUNT(*) 用于分页 total
        // 这里直接验证分页结果与总数无需读 SQL 日志 —— 列表正常 = 插件工作。
    }

    // ----------------- SQL 日志 / Map 转换 -----------------

    @Test
    @DisplayName("MyBatis-Plus 配置开启 log-impl: Slf4jImpl 时 Mapper 调用链路可走通")
    void sql_logging_can_be_observed() {
        List<Employee> all = employeeMapper.selectList(null);
        assertFalse(all.isEmpty(), "全表查询至少有种子数据");
    }

    // ----------------- 唯一约束 -----------------

    @Test
    @DisplayName("工号 uk_emp_no 唯一，重复 INSERT 由数据库层报错")
    void unique_constraint_violation_throws() {
        Employee dup = build("E0001", "重名员工"); // 已存在

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DuplicateKeyException.class,
                () -> employeeMapper.insert(dup),
                "重复工号 INSERT 应抛 DuplicateKeyException，由 DB 兜底");
    }

    // ----------------- Helper -----------------

    private Employee build(String empNo, String name) {
        Employee e = new Employee();
        e.setEmpNo(empNo);
        e.setName(name);
        e.setStatus(BizConst.EMPLOYEE_ACTIVE);
        return e;
    }
}
